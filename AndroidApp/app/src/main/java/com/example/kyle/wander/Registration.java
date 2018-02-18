package com.example.kyle.wander;

import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.view.inputmethod.InputMethodManager;

public class Registration extends AppCompatActivity {

    EditText emailText;
    EditText usernameText;
    EditText passwordText;
    EditText confirmPasswordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        emailText = (EditText) findViewById(R.id.email);
        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);
        confirmPasswordText = (EditText) findViewById(R.id.confirmPassword);
    }

    public void submit(View view){

        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Registration.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        String email = emailText.getText().toString();
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
        String confirmPassword = confirmPasswordText.getText().toString();

        if(!password.equals(confirmPassword)){
            Snackbar.make(findViewById(R.id.myCoordinatorLayout),
                    "Passwords do not match", Snackbar.LENGTH_INDEFINITE).show();
        } else {
            //TODO: Impliment account registration
        }
    }

}
