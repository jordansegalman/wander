package com.example.kyle.wander;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class Login extends AppCompatActivity {
    EditText usernameText;
    EditText passwordText;
    String username;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);



        passwordText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    username = usernameText.getText().toString();
                    password = passwordText.getText().toString();

                    //TODO: username & password validation
                    if(username.equals("username") && password.equals("password")) {
                        startActivity(new Intent(Login.this, AppHome.class));
                    } else{
                        Snackbar.make(findViewById(R.id.myCoordinatorLayout),
                                "Invalid username or Password", Snackbar.LENGTH_INDEFINITE).show();
                    }

                }
                return handled;
            }
        });




    }

    public void facebookButton(View view){
        startActivity(new Intent(Login.this, FacebookLogin.class));
    }

    public void googleButton(View view){
        startActivity(new Intent(Login.this, GoogleLogin.class));
    }

    public void registerButton(View view){
        startActivity(new Intent(Login.this, Registration.class));
    }
}