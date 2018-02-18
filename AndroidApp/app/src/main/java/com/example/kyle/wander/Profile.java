package com.example.kyle.wander;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class Profile extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //TODO: Get profile info from server
        String name = "Test Name";
        String interests = "Test Interests";
        String about = "Test About";

        ImageView profilePicture = (ImageView)findViewById(R.id.picture);
        TextView nameText = (TextView)findViewById(R.id.name);
        TextView interestText = (TextView)findViewById(R.id.interests);
        TextView aboutText = (TextView)findViewById(R.id.about);
        //profilePicture.setBackground();
        nameText.setText(name);
        interestText.setText(interests);
        aboutText.setText(about);

    }

    public void edit(View view){
        startActivity(new Intent(Profile.this, ProfileEdit.class));
    }
}
