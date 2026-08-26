package com.anysoftkeyboard.ime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.R;
import java.util.function.IntConsumer;

public class NavigationStripActionProvider
        implements KeyboardViewContainerView.StripActionProvider {
    @NonNull private final Context mContext;
    @NonNull private final IntConsumer mOnNavKey;

    public NavigationStripActionProvider(@NonNull Context context, @NonNull IntConsumer onNavKey) {
        mContext = context;
        mOnNavKey = onNavKey;
    }

    @Override
    public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
        View root = LayoutInflater.from(mContext).inflate(R.layout.nav_arrows_action, parent, false);
        root.findViewById(R.id.nav_action_home)
                .setOnClickListener(v -> mOnNavKey.accept(KeyCodes.MOVE_HOME));
        root.findViewById(R.id.nav_action_left)
                .setOnClickListener(v -> mOnNavKey.accept(KeyCodes.ARROW_LEFT));
        root.findViewById(R.id.nav_action_right)
                .setOnClickListener(v -> mOnNavKey.accept(KeyCodes.ARROW_RIGHT));
        root.findViewById(R.id.nav_action_end)
                .setOnClickListener(v -> mOnNavKey.accept(KeyCodes.MOVE_END));
        return root;
    }

    @Override
    public void onRemoved() {}
}