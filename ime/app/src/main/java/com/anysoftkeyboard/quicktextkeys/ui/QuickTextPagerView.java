package com.anysoftkeyboard.quicktextkeys.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.anysoftkeyboard.addons.DefaultAddOn;
import com.anysoftkeyboard.ime.InputViewActionsProvider;
import com.anysoftkeyboard.keyboards.PopupListKeyboard;
import com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener;
import com.anysoftkeyboard.keyboards.views.QuickKeysKeyboardView;
import com.anysoftkeyboard.quicktextkeys.HistoryQuickTextKey;
import com.anysoftkeyboard.quicktextkeys.QuickKeyHistoryRecords;
import com.anysoftkeyboard.quicktextkeys.QuickTextKey;
import com.anysoftkeyboard.remote.MediaType;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.astuetz.PagerSlidingTabStrip;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.evendanan.pixel.ViewPagerWithDisable;

public class QuickTextPagerView extends LinearLayout implements InputViewActionsProvider {

  private KeyboardTheme mKeyboardTheme;
  private float mTabTitleTextSize;
  private ColorStateList mTabTitleTextColor;
  private Drawable mCloseKeyboardIcon;
  private Drawable mBackspaceIcon;
  private Drawable mSettingsIcon;
  private Drawable mMediaInsertionDrawable;
  private Drawable mDeleteRecentlyUsedDrawable;
  private int mBottomPadding;
  private QuickKeyHistoryRecords mQuickKeyHistoryRecords;
  private DefaultSkinTonePrefTracker mDefaultSkinTonePrefTracker;
  private DefaultGenderPrefTracker mDefaultGenderPrefTracker;
  private OnKeyboardActionListener mKeyboardActionListener;
  private List<QuickTextKey> mAllQuickTextKeys;
  private View mPagerTabs;
  private ViewPagerWithDisable mViewPager;
  private View mSearchResultsContainer;
  private QuickKeysKeyboardView mSearchKeyboardView;
  private PopupListKeyboard mSearchKeyboard;

  public QuickTextPagerView(Context context) {
    super(context);
  }

  public QuickTextPagerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public QuickTextPagerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private static void setupSlidingTab(
      View rootView,
      float tabTitleTextSize,
      ColorStateList tabTitleTextColor,
      ViewPager pager,
      PagerAdapter adapter,
      ViewPager.OnPageChangeListener onPageChangeListener,
      int startIndex) {
    PagerSlidingTabStrip pagerTabStrip = rootView.findViewById(R.id.pager_tabs);
    pagerTabStrip.setTextSize((int) tabTitleTextSize);
    pagerTabStrip.setTextColor(tabTitleTextColor.getDefaultColor());
    pagerTabStrip.setIndicatorColor(tabTitleTextColor.getDefaultColor());
    pager.setAdapter(adapter);
    pager.setCurrentItem(startIndex);
    pagerTabStrip.setViewPager(pager);
    pagerTabStrip.setOnPageChangeListener(onPageChangeListener);
  }

  public void setThemeValues(
      @NonNull KeyboardTheme keyboardTheme,
      float tabTextSize,
      ColorStateList tabTextColor,
      Drawable closeKeyboardIcon,
      Drawable backspaceIcon,
      Drawable settingsIcon,
      Drawable keyboardDrawable,
      Drawable mediaInsertionDrawable,
      Drawable deleteRecentlyUsedDrawable,
      int bottomPadding,
      Set<MediaType> supportedMediaTypes) {
    mKeyboardTheme = keyboardTheme;
    mTabTitleTextSize = tabTextSize;
    mTabTitleTextColor = tabTextColor;
    mCloseKeyboardIcon = closeKeyboardIcon;
    mBackspaceIcon = backspaceIcon;
    mSettingsIcon = settingsIcon;
    mMediaInsertionDrawable = mediaInsertionDrawable;
    mDeleteRecentlyUsedDrawable = deleteRecentlyUsedDrawable;
    mBottomPadding = bottomPadding;
    findViewById(R.id.quick_keys_popup_quick_keys_insert_media)
        .setVisibility(supportedMediaTypes.isEmpty() ? View.GONE : VISIBLE);
    setBackground(keyboardDrawable);
  }

  @Override
  public void setOnKeyboardActionListener(OnKeyboardActionListener keyboardActionListener) {
    mKeyboardActionListener = keyboardActionListener;
    FrameKeyboardViewClickListener frameKeyboardViewClickListener =
        new FrameKeyboardViewClickListener(keyboardActionListener);
    frameKeyboardViewClickListener.registerOnViews(this);

    final Context context = getContext();
    final List<QuickTextKey> list = new ArrayList<>();
    // always starting with Recent
    final HistoryQuickTextKey historyQuickTextKey =
        new HistoryQuickTextKey(context, mQuickKeyHistoryRecords);
    list.add(historyQuickTextKey);
    // then all the rest
    list.addAll(AnyApplication.getQuickTextKeyFactory(context).getEnabledAddOns());
    mAllQuickTextKeys = list;

    final QuickTextUserPrefs quickTextUserPrefs = new QuickTextUserPrefs(context);

    mViewPager = findViewById(R.id.quick_text_keyboards_pager);
    mPagerTabs = findViewById(R.id.pager_tabs);
    final QuickKeysKeyboardPagerAdapter adapter =
        new QuickKeysKeyboardPagerAdapter(
            context,
            mViewPager,
            list,
            new RecordHistoryKeyboardActionListener(historyQuickTextKey, keyboardActionListener),
            mDefaultSkinTonePrefTracker,
            mDefaultGenderPrefTracker,
            mKeyboardTheme,
            mBottomPadding);

    final ImageView clearEmojiHistoryIcon =
        findViewById(R.id.quick_keys_popup_delete_recently_used_smileys);
    ViewPager.SimpleOnPageChangeListener onPageChangeListener =
        new ViewPager.SimpleOnPageChangeListener() {
          @Override
          public void onPageSelected(int position) {
            super.onPageSelected(position);
            QuickTextKey selectedKey = list.get(position);
            quickTextUserPrefs.setLastSelectedAddOnId(selectedKey.getId());
            // if this is History, we need to show clear icon
            // else, hide the clear icon
            clearEmojiHistoryIcon.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
          }
        };
    int startPageIndex = quickTextUserPrefs.getStartPageIndex(list);
    setupSlidingTab(
        this,
        mTabTitleTextSize,
        mTabTitleTextColor,
        mViewPager,
        adapter,
        onPageChangeListener,
        startPageIndex);

    // setting up icons from theme
    ((ImageView) findViewById(R.id.quick_keys_popup_close)).setImageDrawable(mCloseKeyboardIcon);
    ((ImageView) findViewById(R.id.quick_keys_popup_backspace)).setImageDrawable(mBackspaceIcon);
    ((ImageView) findViewById(R.id.quick_keys_popup_quick_keys_insert_media))
        .setImageDrawable(mMediaInsertionDrawable);
    clearEmojiHistoryIcon.setImageDrawable(mDeleteRecentlyUsedDrawable);
    ((ImageView) findViewById(R.id.quick_keys_popup_quick_keys_settings))
        .setImageDrawable(mSettingsIcon);
    final View actionsLayout = findViewById(R.id.quick_text_actions_layout);
    actionsLayout.setPadding(
        actionsLayout.getPaddingLeft(),
        actionsLayout.getPaddingTop(),
        actionsLayout.getPaddingRight(),
        // this will support the case were we have navigation-bar offset
        actionsLayout.getPaddingBottom() + mBottomPadding);

    setupSearch(context);
  }

  private void setupSearch(Context context) {
    mSearchResultsContainer = findViewById(R.id.quick_text_search_results);
    final EditText searchEdit = findViewById(R.id.quick_text_search);
    searchEdit.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            onSearchQueryChanged(s.toString());
          }
        });
  }

  private void onSearchQueryChanged(String query) {
    if (TextUtils.isEmpty(query)) {
      mSearchResultsContainer.setVisibility(View.GONE);
      mPagerTabs.setVisibility(View.VISIBLE);
      mViewPager.setVisibility(View.VISIBLE);
      return;
    }

    final String lowerQuery = query.toLowerCase(Locale.getDefault());
    final List<String> names = new ArrayList<>();
    final List<String> values = new ArrayList<>();
    for (QuickTextKey key : mAllQuickTextKeys) {
      if (key.isPopupKeyboardUsed()) continue;
      final List<String> keyNames = key.getPopupListNames();
      final List<String> keyValues = key.getPopupListValues();
      final int count = Math.min(keyNames.size(), keyValues.size());
      for (int i = 0; i < count; i++) {
        final String name = keyNames.get(i);
        if (name != null && name.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
          names.add(name);
          values.add(keyValues.get(i));
        }
      }
    }

    if (names.isEmpty()) {
      names.add("...");
      values.add("");
    }

    showSearchResults(names, values);
  }

  private void showSearchResults(List<String> names, List<String> values) {
    if (mSearchKeyboardView == null) {
      final View searchRoot =
          LayoutInflater.from(getContext())
              .inflate(R.layout.quick_text_popup_autorowkeyboard_view, (FrameLayout) mSearchResultsContainer, true);
      mSearchKeyboardView = searchRoot.findViewById(R.id.keys_container);
      mSearchKeyboardView.setKeyboardTheme(mKeyboardTheme);
      mSearchKeyboardView.setOnKeyboardActionListener(mKeyboardActionListener);
    }

    final DefaultAddOn defaultAddOn = new DefaultAddOn(getContext(), getContext());
    mSearchKeyboard =
        new PopupListKeyboard(
            defaultAddOn,
            getContext(),
            mSearchKeyboardView.getThemedKeyboardDimens(),
            names,
            values,
            getContext().getString(R.string.search_emojis_hint));
    mSearchKeyboardView.setKeyboard(mSearchKeyboard);

    mSearchResultsContainer.setVisibility(View.VISIBLE);
    mPagerTabs.setVisibility(View.GONE);
    mViewPager.setVisibility(View.GONE);
  }

  public void setQuickKeyHistoryRecords(QuickKeyHistoryRecords quickKeyHistoryRecords) {
    mQuickKeyHistoryRecords = quickKeyHistoryRecords;
  }

  public void setEmojiVariantsPrefTrackers(
      DefaultSkinTonePrefTracker defaultSkinTonePrefTracker,
      DefaultGenderPrefTracker defaultGenderPrefTracker) {
    mDefaultSkinTonePrefTracker = defaultSkinTonePrefTracker;
    mDefaultGenderPrefTracker = defaultGenderPrefTracker;
  }
}
