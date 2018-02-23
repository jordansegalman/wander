package com.example.kyle.wander;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

public class Settings extends AppCompatActivity {

    Switch locationSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        boolean locationTracking = sharedPref.getBoolean("tracking", true);

        locationSwitch = (Switch) findViewById(R.id.tracking);
        locationSwitch.setChecked(locationTracking);

    }

    public void locationToggle(View view){
        boolean value = locationSwitch.isChecked();
        //TODO: turn off location services. "value" indicates whether it should be on(true) or off(false)
        //This code is run every time the user hits the button on the settings page

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("tracking", value);
        editor.commit();

    }

    public void delete(View view){
        startActivity(new Intent(Settings.this, Delete.class));
    }

    public void changeEmail(View view){
        startActivity(new Intent(Settings.this, ChangeEmail.class));
    }

    public void changePassword(View view){
        startActivity(new Intent(Settings.this, ChangePassword.class));
    }


    @Override
    public void onBackPressed() {
        startActivity(new Intent(Settings.this, AppHome.class));
    }

}
