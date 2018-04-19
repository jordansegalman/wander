package me.vvander.wander;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class ChangeThemeActivity extends AppCompatActivity {
    private static final String SP_THEME = "theme";
    Spinner colorsSpinner;
    Spinner fontsSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_theme);

        colorsSpinner = findViewById(R.id.colorsSpinner);
        fontsSpinner = findViewById(R.id.fontsSpinner);

        SharedPreferences sharedPreferences = getSharedPreferences(SP_THEME, Context.MODE_PRIVATE);
        String color = sharedPreferences.getString("color", "Default");
        String font = sharedPreferences.getString("font", "Sans-Serif");

        colorsSpinner.setSelection(((ArrayAdapter) colorsSpinner.getAdapter()).getPosition(color));
        fontsSpinner.setSelection(((ArrayAdapter) fontsSpinner.getAdapter()).getPosition(font));
    }

    public void save(View view) {
        String color = colorsSpinner.getSelectedItem().toString();
        String font = fontsSpinner.getSelectedItem().toString();

        SharedPreferences sharedPreferences = getSharedPreferences(SP_THEME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("color", color);
        editor.putString("font", font);
        editor.apply();

        Intent intent = new Intent(ChangeThemeActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void onBackPressed() {
        startActivity(new Intent(ChangeThemeActivity.this, SettingsActivity.class));
        finish();
    }
}