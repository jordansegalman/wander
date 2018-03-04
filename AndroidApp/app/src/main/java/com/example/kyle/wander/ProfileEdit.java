package com.example.kyle.wander;

import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class ProfileEdit extends AppCompatActivity {
    private EditText etInterests;
    private EditText etAbout;
    private EditText etLocation;
    private EditText etEmail;
    private EditText etName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        etInterests = (EditText) findViewById(R.id.interests_text);
        etAbout = (EditText) findViewById(R.id.about_text);
        etLocation = (EditText) findViewById(R.id.location_text);
        etEmail = (EditText) findViewById(R.id.location_text);
        etName = (EditText) findViewById(R.id.name);

        Intent in = getIntent();
        String loc = in.getExtras().getString("location");
        String about = in.getExtras().getString("about");
        String interests = in.getExtras().getString("interests");
        String name = in.getExtras().getString("name");
        String email = in.getExtras().getString("email");

        etInterests.setText(interests);
        etAbout.setText(about);
        etLocation.setText(loc);
        etName.setText(name);
        etEmail.setText(email);

    }

    public void done(View view){
        //TODO: Save profile data
        etInterests = (EditText) findViewById(R.id.interests_text);
        etAbout = (EditText) findViewById(R.id.about_text);
        etLocation = (EditText) findViewById(R.id.location_text);
        etEmail = (EditText) findViewById(R.id.location_text);
        etName = (EditText) findViewById(R.id.name);

        Intent i = new Intent(ProfileEdit.this, Profile.class);
        i.putExtra("location", etLocation.getText().toString());
        i.putExtra("about", etAbout.getText().toString());
        i.putExtra("interests", etInterests.getText().toString());
        i.putExtra("email", etEmail.getText().toString());
        i.putExtra("name", etName.getText().toString());

        ActivityCompat.startActivityForResult(this, i, 0, null);
        //startActivity(i);
    }
}
