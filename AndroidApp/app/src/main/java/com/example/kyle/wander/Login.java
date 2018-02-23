package com.example.kyle.wander;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {

    private RequestQueue requestQueue;
    private String url;

    EditText usernameText;
    EditText passwordText;
    String username;
    String password;

    // Since login is the startup activity, I will set the singleton class (Data) in here.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeData();

        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);

        requestQueue = Volley.newRequestQueue(this);
        url = Data.getInstance().getUrl() + "/login";


        passwordText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    username = usernameText.getText().toString();
                    password = passwordText.getText().toString();

                    //TODO: username & password validation
/*
                    if(username.equals("username") && password.equals("password")) {
                        startActivity(new Intent(Login.this, MyLocation.class));
                    } else{
                        Snackbar.make(findViewById(R.id.myCoordinatorLayout),
                                "Invalid username or Password", Snackbar.LENGTH_INDEFINITE).show();
                    }
*/
                }
                return handled;
            }
        });
    }

    public void login(View view){
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Registration.INPUT_METHOD_SERVICE);



    Intent i = new Intent(this, GpsCollection.class);
    startService(i);



        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);


        //NEXT TWO LINES ARE FOR TESTING ONLY
        Intent intent = new Intent(Login.this, MyLocation.class);//Change myLocation to AppHome
        startActivity(intent);
        //END OF TESTING



        sendPOSTRequest();
    }

    /*
    Initialize all variables that we plan to share between classes. These
    variables are all stored in the singleton class Data.java
     */
    public void initializeData() {
        // When using an android simulator, 127.0.0.1 refers to the emulator's own
        // loopback address, so you should use 10.0.2.2. We will change this when
        // we get an actual server
        Data.getInstance().setUrl("http://10.0.2.2:3000");
    }

    private void sendPOSTRequest() {
        Map<String, String> params = new HashMap<String,String>();
        params.put("username", usernameText.getText().toString());
        params.put("password", passwordText.getText().toString());

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // still need a check here
                        Toast.makeText(getApplicationContext(), "Login Successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Login.this, AppHome.class);//Change myLocation to AppHome
                        startActivity(intent);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error: ", error.toString());
                    }
                }
        );
        requestQueue.add(postRequest);
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