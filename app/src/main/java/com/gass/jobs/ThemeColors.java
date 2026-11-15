package com.gass.jobs;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

final class ThemeColors {
    private ThemeColors() {}

    static final String[] KEYS = {"gold_dark", "blue_dark", "amber", "red_dark", "green_matrix", "purple_dark",
            "midnight_blue", "emerald_dark", "amoled_black", "glass_blue", "glass_amber", "glass_violet", "glass_emerald"};
    static final String[] LABELS = {"Gold Dark", "Blue Dark", "Amber", "Red Dark", "Green Matrix", "Purple Dark",
            "Midnight Blue", "Emerald Dark", "AMOLED Black", "Glass Blue", "Glass Amber Gold", "Glass Violet", "Glass Emerald"};

    /** returns {canvas, panel, accent, accentDark, glass(0/1)} */
    static int[] get(String k) {
        if ("blue_dark".equals(k)) return new int[]{Color.rgb(7, 15, 30), Color.rgb(17, 31, 53), Color.rgb(75, 164, 255), Color.rgb(25, 91, 177), 0};
        if ("red_dark".equals(k)) return new int[]{Color.rgb(25, 9, 13), Color.rgb(48, 19, 26), Color.rgb(255, 91, 105), Color.rgb(174, 34, 54), 0};
        if ("amber".equals(k)) return new int[]{Color.rgb(29, 20, 8), Color.rgb(53, 37, 16), Color.rgb(255, 181, 55), Color.rgb(188, 105, 18), 0};
        if ("green_matrix".equals(k)) return new int[]{Color.rgb(3, 14, 9), Color.rgb(9, 30, 18), Color.rgb(66, 255, 137), Color.rgb(0, 135, 67), 0};
        if ("purple_dark".equals(k)) return new int[]{Color.rgb(17, 10, 29), Color.rgb(34, 22, 53), Color.rgb(190, 130, 255), Color.rgb(112, 62, 177), 0};
        if ("midnight_blue".equals(k)) return new int[]{Color.rgb(5, 10, 24), Color.rgb(14, 26, 52), Color.rgb(96, 165, 250), Color.rgb(37, 99, 235), 0};
        if ("emerald_dark".equals(k)) return new int[]{Color.rgb(4, 16, 13), Color.rgb(10, 36, 30), Color.rgb(52, 211, 153), Color.rgb(5, 150, 105), 0};
        if ("amoled_black".equals(k)) return new int[]{Color.rgb(0, 0, 0), Color.rgb(16, 16, 18), Color.rgb(0, 229, 255), Color.rgb(0, 151, 178), 0};
        if ("glass_blue".equals(k)) return new int[]{Color.rgb(6, 12, 28), Color.rgb(28, 54, 100), Color.rgb(120, 195, 255), Color.rgb(45, 120, 230), 1};
        if ("glass_amber".equals(k)) return new int[]{Color.rgb(20, 13, 5), Color.rgb(74, 52, 20), Color.rgb(255, 205, 90), Color.rgb(214, 140, 30), 1};
        if ("glass_violet".equals(k)) return new int[]{Color.rgb(13, 8, 26), Color.rgb(58, 36, 100), Color.rgb(205, 160, 255), Color.rgb(124, 77, 214), 1};
        if ("glass_emerald".equals(k)) return new int[]{Color.rgb(3, 15, 12), Color.rgb(16, 64, 52), Color.rgb(80, 230, 170), Color.rgb(16, 160, 110), 1};
        return new int[]{Color.rgb(10, 11, 14), Color.rgb(25, 27, 33), Color.rgb(255, 190, 45), Color.rgb(222, 145, 8), 0};
    }

    static String label(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return LABELS[i];
        return LABELS[0];
    }

    static int mix(int a, int b, float t) {
        return Color.rgb(
                (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }

    static int withAlpha(int c, int alpha) {
        return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
    }

    static Drawable background(int[] c) {
        if (c[4] != 1) return new ColorDrawable(c[0]);
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{mix(c[0], c[3], 0.45f), c[0], mix(c[0], c[2], 0.18f)});
    }
}
