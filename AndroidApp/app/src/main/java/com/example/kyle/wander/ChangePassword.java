package com.example.kyle.wander;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.util.Set;

public class ChangePassword extends AppCompatActivity {

    EditText passwordEdit;
    EditText confirmEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        passwordEdit = (EditText)findViewById(R.id.password);
        confirmEdit = (EditText)findViewById(R.id.confirm);
    }

    public void done(View view){
        String password = passwordEdit.getText().toString();
        String confirmPassword = confirmEdit.getText().toString();


        if(!password.equals(confirmPassword)){
            Snackbar.make(findViewById(R.id.myCoordinatorLayout),
                    "Passwords do not match", Snackbar.LENGTH_INDEFINITE).show();
        } else {
            //TODO: Update account in database

            startActivity(new Intent(ChangePassword.this, Settings.class));
        }

    }
}
