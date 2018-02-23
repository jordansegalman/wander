package com.example.kyle.wander;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class ChangeEmail extends AppCompatActivity {

    EditText emailEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

        emailEdit = (EditText)findViewById(R.id.email);
    }

    public void done(View view){
        String email = emailEdit.getText().toString();
        //TODO: update email in database. Email string is stored in "email"

        startActivity(new Intent(ChangeEmail.this, Settings.class));
    }
}
