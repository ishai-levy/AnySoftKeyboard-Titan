package com.anysoftkeyboard.ime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.R;

public class TitanToolbarController
        implements KeyboardViewContainerView.StripActionProvider {

    private View mActionView;
    private TextView mLanguageButton;
    private TextView mHamburgerButton;

    @NonNull private final View.OnClickListener mOnHyphen;
    @NonNull private final View.OnClickListener mOnComma;
    @NonNull private final View.OnClickListener mOnApostrophe;
    @NonNull private final View.OnClickListener mOnExclamation;
    @NonNull private final View.OnClickListener mOnQuestion;
    @NonNull private final View.OnClickListener mOnPeriod;
    @NonNull private final View.OnClickListener mOnSymbols;
    @NonNull private final View.OnClickListener mOnLanguage;
    @NonNull private final View.OnClickListener mOnCopy;
    @NonNull private final View.OnClickListener mOnPaste;
    @NonNull private final View.OnClickListener mOnSelectAll;
    @NonNull private final View.OnClickListener mOnDelete;
    @NonNull private final View.OnClickListener mOnArrowLeft;
    @NonNull private final View.OnClickListener mOnArrowRight;
    @NonNull private final View.OnClickListener mOnArrowUp;
    @NonNull private final View.OnClickListener mOnArrowDown;
    @NonNull private final Runnable mOnCloseOtherMenus;

    public TitanToolbarController(
            @NonNull View.OnClickListener onHyphen,
            @NonNull View.OnClickListener onComma,
            @NonNull View.OnClickListener onApostrophe,
            @NonNull View.OnClickListener onExclamation,
            @NonNull View.OnClickListener onQuestion,
            @NonNull View.OnClickListener onPeriod,
            @NonNull View.OnClickListener onSymbols,
            @NonNull View.OnClickListener onLanguage,
            @NonNull View.OnClickListener onCopy,
            @NonNull View.OnClickListener onPaste,
            @NonNull View.OnClickListener onSelectAll,
            @NonNull View.OnClickListener onDelete,
            @NonNull View.OnClickListener onArrowLeft,
            @NonNull View.OnClickListener onArrowRight,
            @NonNull View.OnClickListener onArrowUp,
            @NonNull View.OnClickListener onArrowDown,
            @NonNull Runnable onCloseOtherMenus) {
        mOnHyphen = onHyphen;
        mOnComma = onComma;
        mOnApostrophe = onApostrophe;
        mOnExclamation = onExclamation;
        mOnQuestion = onQuestion;
        mOnPeriod = onPeriod;
        mOnSymbols = onSymbols;
        mOnLanguage = onLanguage;
        mOnCopy = onCopy;
        mOnPaste = onPaste;
        mOnSelectAll = onSelectAll;
        mOnDelete = onDelete;
        mOnArrowLeft = onArrowLeft;
        mOnArrowRight = onArrowRight;
        mOnArrowUp = onArrowUp;
        mOnArrowDown = onArrowDown;
        mOnCloseOtherMenus = onCloseOtherMenus;
    }

    @Override
    public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
        mActionView =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.titan_toolbar_action, parent, false);

        bindBottomRow(mActionView);
        bindExpandedGrid(mActionView);

        return mActionView;
    }

    public View getActionView() {
        return mActionView;
    }

    private void bindBottomRow(@NonNull View root) {
        View bottom = root.findViewById(R.id.titan_toolbar_bottom);

        bottom.findViewById(R.id.titan_toolbar_hyphen).setOnClickListener(mOnHyphen);
        bottom.findViewById(R.id.titan_toolbar_comma).setOnClickListener(mOnComma);
        bottom.findViewById(R.id.titan_toolbar_apostrophe).setOnClickListener(mOnApostrophe);
        bottom.findViewById(R.id.titan_toolbar_exclamation).setOnClickListener(mOnExclamation);
        bottom.findViewById(R.id.titan_toolbar_question).setOnClickListener(mOnQuestion);
        bottom.findViewById(R.id.titan_toolbar_period).setOnClickListener(mOnPeriod);
        bottom.findViewById(R.id.titan_toolbar_symbols).setOnClickListener(mOnSymbols);

        mLanguageButton = bottom.findViewById(R.id.titan_toolbar_language);
        mLanguageButton.setOnClickListener(mOnLanguage);

        mHamburgerButton = bottom.findViewById(R.id.titan_toolbar_hamburger);
        mHamburgerButton.setOnClickListener(v -> toggleExpanded(root));
    }

    private void bindExpandedGrid(@NonNull View root) {
        View expanded = root.findViewById(R.id.titan_toolbar_expanded);

        expanded.findViewById(R.id.titan_toolbar_copy).setOnClickListener(mOnCopy);
        expanded.findViewById(R.id.titan_toolbar_paste).setOnClickListener(mOnPaste);
        expanded.findViewById(R.id.titan_toolbar_select_all).setOnClickListener(mOnSelectAll);
        expanded.findViewById(R.id.titan_toolbar_delete).setOnClickListener(mOnDelete);
        expanded.findViewById(R.id.titan_toolbar_left).setOnClickListener(mOnArrowLeft);
        expanded.findViewById(R.id.titan_toolbar_right).setOnClickListener(mOnArrowRight);
        expanded.findViewById(R.id.titan_toolbar_up).setOnClickListener(mOnArrowUp);
        expanded.findViewById(R.id.titan_toolbar_down).setOnClickListener(mOnArrowDown);
    }

    private void toggleExpanded(@NonNull View root) {
        View expanded = root.findViewById(R.id.titan_toolbar_expanded);
        boolean isExpanded = expanded.getVisibility() == View.VISIBLE;
        if (!isExpanded) {
            mOnCloseOtherMenus.run();
        }
        expanded.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
        // When the expanded menu is open it sits above the bottom row, so the button shows a
        // downward chevron. When closed it shows the hamburger icon.
        mHamburgerButton.setText(isExpanded ? "☰" : "∨");
        root.requestLayout();
    }

    public void closeExpanded() {
        if (mActionView == null) return;
        View expanded = mActionView.findViewById(R.id.titan_toolbar_expanded);
        if (expanded.getVisibility() == View.VISIBLE) {
            expanded.setVisibility(View.GONE);
            mHamburgerButton.setText("☰");
            mActionView.requestLayout();
        }
    }

    public boolean isExpanded() {
        if (mActionView == null) return false;
        return mActionView.findViewById(R.id.titan_toolbar_expanded).getVisibility() == View.VISIBLE;
    }

    public void setLanguageText(@NonNull String languageCode) {
        if (mLanguageButton != null) {
            mLanguageButton.setText(languageCode);
        }
    }

    @Override
    public void onRemoved() {}
}
