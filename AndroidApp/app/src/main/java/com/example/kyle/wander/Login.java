package com.example.kyle.wander;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
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
                    //TODO: username & password validation. User input for username is stored in 'username'
                    // and Password is in 'password'

                    handled = true;
                }
                return handled;
            }
        });




    }
}