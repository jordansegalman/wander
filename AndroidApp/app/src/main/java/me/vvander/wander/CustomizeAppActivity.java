package me.vvander.wander;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class CustomizeAppActivity extends AppCompatActivity {

    Spinner colors;
    Spinner fonts;
    private static final String SP_CUSTOMIZATION = "UiCustomization";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Customize.getCustomTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_app);

        colors = (Spinner) findViewById(R.id.colorSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.colorList,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colors.setAdapter(adapter);

        fonts = (Spinner) findViewById(R.id.fontSpinner);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.fontList,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fonts.setAdapter(adapter2);

        SharedPreferences sharedPref = getSharedPreferences(SP_CUSTOMIZATION, Context.MODE_PRIVATE);
        String color = sharedPref.getString("color", "Default");
        String font = sharedPref.getString("font", "Default");

        colors.setSelection(adapter.getPosition(color));
        fonts.setSelection(adapter2.getPosition(font));
    }

    public void save(View view){
        String color = colors.getSelectedItem().toString();
        String font = fonts.getSelectedItem().toString();

        SharedPreferences sharedPreferences = getSharedPreferences(SP_CUSTOMIZATION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("color", color);
        editor.putString("font", font);
        editor.apply();

        startActivity(new Intent(CustomizeAppActivity.this, SettingsActivity.class));
    }

    public void onBackPressed(){
        startActivity(new Intent(CustomizeAppActivity.this, SettingsActivity.class));
    }
}
