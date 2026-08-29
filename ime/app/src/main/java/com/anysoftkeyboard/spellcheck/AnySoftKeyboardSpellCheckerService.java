/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.spellcheck;

import android.service.textservice.SpellCheckerService;
import android.text.TextUtils;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.Dictionary;
import com.anysoftkeyboard.dictionaries.DictionaryAddOnAndBuilder;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.menny.android.anysoftkeyboard.AnyApplication;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements Android's spell-checker-service framework (distinct from the IME's own
 * suggestion/candidate-strip engine) on top of the same per-locale dictionaries already used while
 * typing. This lets AnySoftKeyboard be selected as the system spell checker (Settings -&gt; System
 * -&gt; Languages &amp; input -&gt; Spell checker), giving red-squiggly-underline spell checking in any
 * app, for any language this build has a dictionary for - regardless of which language is
 * currently active as the on-screen/physical keyboard.
 */
public class AnySoftKeyboardSpellCheckerService extends SpellCheckerService {

  private static final String TAG = "ASKSpellChecker";

  @Override
  public Session createSession() {
    return new AnySoftKeyboardSpellCheckerSession();
  }

  private class AnySoftKeyboardSpellCheckerSession extends Session {

    // Cache of already-loaded dictionaries for this session, keyed by the resolved locale
    // string (e.g. "en", "iw"). A session usually only ever needs one or two locales (whatever
    // languages the user actually types in the app they're using), so this stays small.
    // ConcurrentHashMap because Android may call this session's methods from multiple threads
    // concurrently (per the SpellCheckerService framework docs).
    private final Map<String, Dictionary> mLoadedDictionaries = new ConcurrentHashMap<>();

    // Locales we've already tried to resolve to a dictionary and failed - so we don't repeat
    // the (cheap, but not free) lookup and loading-attempt on every subsequent word.
    private final Map<String, Boolean> mNoDictionaryForLocale = new ConcurrentHashMap<>();

    @Override
    public void onCreate() {
      // nothing to eagerly set up: dictionaries are resolved and loaded lazily, per requested
      // locale, on first use - since a session may never actually need most locales.
    }

    @Override
    public SuggestionsInfo onGetSuggestions(TextInfo textInfo, int suggestionsLimit) {
      final CharSequence word = textInfo == null ? null : textInfo.getText();
      if (TextUtils.isEmpty(word)) {
        return emptyResult();
      }

      final Dictionary dictionary = getDictionaryForCurrentLocale();
      if (dictionary == null) {
        // No installed language pack matches the requested locale: say nothing is misspelled,
        // rather than guessing with the wrong language's dictionary.
        return emptyResult();
      }

      if (dictionary.isValidWord(word)) {
        return new SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, new String[0]);
      }

      final List<String> suggestions = getSuggestionsFromDictionary(dictionary, word, suggestionsLimit);
      return new SuggestionsInfo(
          SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO, suggestions.toArray(new String[0]));
    }

    @Override
    public SuggestionsInfo[] onGetSuggestionsMultiple(
        TextInfo[] textInfos, int suggestionsLimit, boolean sequentialWords) {
      final SuggestionsInfo[] results = new SuggestionsInfo[textInfos.length];
      for (int i = 0; i < textInfos.length; i++) {
        results[i] = onGetSuggestions(textInfos[i], suggestionsLimit);
      }
      return results;
    }

    @Override
    public void onClose() {
      for (Dictionary dictionary : mLoadedDictionaries.values()) {
        dictionary.close();
      }
      mLoadedDictionaries.clear();
      mNoDictionaryForLocale.clear();
    }

    @NonNull
    private SuggestionsInfo emptyResult() {
      return new SuggestionsInfo(0, new String[0]);
    }

    @Nullable
    private Dictionary getDictionaryForCurrentLocale() {
      final String requestedLocale = getLocale();
      final String language = extractLanguageCode(requestedLocale);
      if (TextUtils.isEmpty(language)) {
        return null;
      }

      Dictionary cached = mLoadedDictionaries.get(language);
      if (cached != null) {
        return cached;
      }
      if (Boolean.TRUE.equals(mNoDictionaryForLocale.get(language))) {
        return null;
      }

      Dictionary loaded = loadDictionaryForLanguage(language);
      if (loaded == null) {
        // "he" (modern ISO code) vs "iw" (legacy code some AnySoftKeyboard dictionary packs,
        // e.g. Hebrew, still declare their locale as) - try the other spelling before giving up.
        final String altLanguage = alternateHebrewCode(language);
        if (altLanguage != null) {
          loaded = loadDictionaryForLanguage(altLanguage);
        }
      }

      if (loaded == null) {
        mNoDictionaryForLocale.put(language, Boolean.TRUE);
        return null;
      }
      mLoadedDictionaries.put(language, loaded);
      return loaded;
    }

    @Nullable
    private String alternateHebrewCode(String language) {
      if ("he".equals(language)) return "iw";
      if ("iw".equals(language)) return "he";
      return null;
    }

    /**
     * getLocale() returns a Java-Locale-style string ("en_US", "he_IL") or, on some versions, a
     * BCP-47 tag ("en-US"). We only care about the language subtag - splitting on either
     * separator and lower-casing is more robust here than trying Locale's own parsing, since
     * {@code new Locale("en_US")} does NOT split that into language+country - it treats the
     * whole string as a single (invalid) language code.
     */
    @Nullable
    private String extractLanguageCode(@Nullable String localeString) {
      if (TextUtils.isEmpty(localeString)) {
        return null;
      }
      final String[] parts = localeString.split("[_-]", 2);
      return parts[0].toLowerCase(Locale.ROOT);
    }

    @Nullable
    private Dictionary loadDictionaryForLanguage(String language) {
      final ExternalDictionaryFactory factory =
          AnyApplication.getExternalDictionaryFactory(AnySoftKeyboardSpellCheckerService.this);
      final DictionaryAddOnAndBuilder builder = factory.getDictionaryBuilderByLocale(language);
      if (builder == null) {
        return null;
      }
      try {
        final Dictionary dictionary = builder.createDictionary();
        // Synchronous on purpose: SpellCheckerService.Session callbacks already run off the
        // main thread, and the result is cached per-session, so this only blocks once per
        // language actually encountered.
        dictionary.loadDictionary();
        return dictionary;
      } catch (Exception e) {
        Logger.e(TAG, e, "Failed to create/load dictionary for language %s", language);
        return null;
      }
    }

    @NonNull
    private List<String> getSuggestionsFromDictionary(
        Dictionary dictionary, CharSequence word, int suggestionsLimit) {
      final WordComposer composer = new WordComposer();
      final int length = word.length();
      for (int i = 0; i < length; ) {
        final int codePoint = Character.codePointAt(word, i);
        composer.add(codePoint, new int[] {codePoint});
        i += Character.charCount(codePoint);
      }

      final List<String> suggestions = new ArrayList<>(suggestionsLimit);
      dictionary.getSuggestions(
          composer,
          (chars, wordOffset, wordLength, frequency, from) -> {
            if (suggestions.size() >= suggestionsLimit) {
              return false;
            }
            suggestions.add(new String(chars, wordOffset, wordLength));
            return true;
          });
      return suggestions;
    }
  }
}
