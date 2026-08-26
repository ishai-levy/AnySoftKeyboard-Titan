package com.anysoftkeyboard.ime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.R;

public class ShowKeyboardStripActionProvider
        implements KeyboardViewContainerView.StripActionProvider {
    @NonNull private final Context mContext;
    @NonNull private final Runnable mOnClick;

    public ShowKeyboardStripActionProvider(@NonNull Context context, @NonNull Runnable onClick) {
        mContext = context;
        mOnClick = onClick;
    }

    @Override
    public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
        View root =
                LayoutInflater.from(mContext).inflate(R.layout.show_keyboard_action, parent, false);
        root.setOnClickListener(v -> mOnClick.run());
        return root;
    }

    @Override
    public void onRemoved() {}
}