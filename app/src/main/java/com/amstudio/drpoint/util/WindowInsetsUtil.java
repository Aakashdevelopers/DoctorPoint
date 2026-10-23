package com.amstudio.drpoint.util;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class WindowInsetsUtil {

    /**
     * Applies system bar insets (status bar / notch at top, navigation bar at bottom)
     * as padding to the given root view while preserving initial XML paddings.
     */
    public static void applySystemBarInsets(View rootView) {
        if (rootView == null) return;
        final int initialPaddingLeft = rootView.getPaddingLeft();
        final int initialPaddingTop = rootView.getPaddingTop();
        final int initialPaddingRight = rootView.getPaddingRight();
        final int initialPaddingBottom = rootView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialPaddingLeft + insets.left,
                    initialPaddingTop + insets.top,
                    initialPaddingRight + insets.right,
                    initialPaddingBottom + insets.bottom
            );
            return windowInsets;
        });
    }
}
