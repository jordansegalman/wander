package me.vvander.wander;

import android.content.Context;
import android.content.SharedPreferences;

public class Theme {
    private static final String SP_THEME = "theme";

    public static int getTheme(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_THEME, Context.MODE_PRIVATE);
        String color = sharedPreferences.getString("color", "Default");
        String font = sharedPreferences.getString("font", "Sans-Serif");
        if (font.equalsIgnoreCase("Serif")) {
            switch (color) {
                case "Red":
                    return R.style.AppTheme_Red_serif;
                case "Orange":
                    return R.style.AppTheme_Orange_serif;
                case "Yellow":
                    return R.style.AppTheme_Yellow_serif;
                case "Green":
                    return R.style.AppTheme_Green_serif;
                case "Blue":
                    return R.style.AppTheme_Blue_serif;
                case "Purple":
                    return R.style.AppTheme_Purple_serif;
                default:
                    return R.style.AppTheme_serif;
            }
        } else {
            switch (color) {
                case "Red":
                    return R.style.AppTheme_Red;
                case "Orange":
                    return R.style.AppTheme_Orange;
                case "Yellow":
                    return R.style.AppTheme_Yellow;
                case "Green":
                    return R.style.AppTheme_Green;
                case "Blue":
                    return R.style.AppTheme_Blue;
                case "Purple":
                    return R.style.AppTheme_Purple;
                default:
                    return R.style.AppTheme;
            }
        }
    }

    public static int getThemeNoActionBar(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(SP_THEME, Context.MODE_PRIVATE);
        String color = sharedPref.getString("color", "Default");
        String font = sharedPref.getString("font", "Sans-Serif");
        if(font.equalsIgnoreCase("Serif")) {
            switch (color) {
                case "Red":
                    return R.style.AppTheme_NoActionBar_Red_serif;
                case "Orange":
                    return R.style.AppTheme_NoActionBar_Orange_serif;
                case "Yellow":
                    return R.style.AppTheme_NoActionBar_Yellow_serif;
                case "Green":
                    return R.style.AppTheme_NoActionBar_Green_serif;
                case "Blue":
                    return R.style.AppTheme_NoActionBar_Blue_serif;
                case "Purple":
                    return R.style.AppTheme_NoActionBar_Purple_serif;
                default:
                    return R.style.AppTheme_NoActionBar_serif;
            }
        } else {
            switch (color) {
                case "Red":
                    return R.style.AppTheme_NoActionBar_Red;
                case "Orange":
                    return R.style.AppTheme_NoActionBar_Orange;
                case "Yellow":
                    return R.style.AppTheme_NoActionBar_Yellow;
                case "Green":
                    return R.style.AppTheme_NoActionBar_Green;
                case "Blue":
                    return R.style.AppTheme_NoActionBar_Blue;
                case "Purple":
                    return R.style.AppTheme_NoActionBar_Purple;
                default:
                    return R.style.AppTheme_NoActionBar;
            }
        }
    }
}