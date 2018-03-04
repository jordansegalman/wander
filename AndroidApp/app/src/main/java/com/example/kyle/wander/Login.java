package com.example.kyle.wander;

import android.content.Context;
import android.content.Intent;
import android.icu.util.Output;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class Login extends AppCompatActivity {

    private RequestQueue requestQueue;

    EditText usernameText;
    EditText passwordText;
    //String username;
    //String password;

    // Since login is the startup activity, I will set the singleton class (Data) in here.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeData();

        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);

        requestQueue = Volley.newRequestQueue(this);

        checkSession();

        passwordText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //username = usernameText.getText().toString();
                    //password = passwordText.getText().toString();

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
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
    }

    public void login(View view){
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Registration.INPUT_METHOD_SERVICE);




        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);


        //NEXT TWO LINES ARE FOR TESTING ONLY
        //Intent intent = new Intent(Login.this, MyLocation.class);//Change myLocation to AppHome
        //startActivity(intent);
        //END OF TESTING
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
        if (username.length() == 0 || password.length() == 0) {
            Snackbar.make(findViewById(R.id.myCoordinatorLayout),
                    "Please enter a password and a username.", Snackbar.LENGTH_LONG).show();
            return;
        }

        attemptLogin(username, password);
    }

    /*
    Initialize all variables that we plan to share between classes. These
    variables are all stored in the singleton class Data.java
     */
    public void initializeData() {
        // When using an android simulator, 127.0.0.1 refers to the emulator's own
        // loopback address, so you should use 10.0.2.2. We will change this when
        // we get an actual server
        //Data.getInstance().setUrl("http://10.0.2.2:3000");
    }

    public void checkSession() {
        String fileName = Data.getInstance().getSessionInfoFile();
        try
        {
            Log.d("Tag", "GOT HERE!!");
            File check = getBaseContext().getFileStreamPath(fileName);
            if (check.exists()) {
                Log.d("Tag", "FILE EXISTS!!");
                FileInputStream fs = getBaseContext().openFileInput(fileName);
                InputStreamReader ir = new InputStreamReader(fs);
                BufferedReader br = new BufferedReader(ir);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                Log.d("This is the read in: ", sb.toString());
                checkSessionId(sb.toString());



                // File exists
            } else {
                Log.d("Tag", "FILE DOES NOT EXIST!!");

                // File does not exist
                File file = new File(Data.getInstance().getSessionInfoFile());
                FileOutputStream outputStream = openFileOutput(fileName, MODE_PRIVATE);
                outputStream.close();
                //Toast.makeText(getApplicationContext(), "Successful!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void attemptLogin(String username, String password) {
        Map<String, String> params = new HashMap<String,String>();
        params.put("username", username);
        params.put("password", password);

        //FOLLOWING LINE IS FOR TESTING DO NOT UNCOMMENT
        //startActivity(new Intent(Login.this, AppHome.class));

        //set the Data username when login
        Data.getInstance().setUsername(username);
        String url = Data.getInstance().getUrl() + "/login";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Login Successful!", Toast.LENGTH_SHORT).show();
                                String email = response.getString("email");
                                Data.getInstance().setEmail(email);

                                String session = response.getString("session_id");
                                Data.getInstance().setSessionId(session);
                                try {
                                    FileOutputStream fos = openFileOutput(Data.getInstance().getSessionInfoFile(), Context.MODE_PRIVATE);
                                    fos.write(session.getBytes());
                                    fos.close();
                                } catch(IOException e) {
                                    e.printStackTrace();
                                }
                                startGPSService();
                                Intent intent = new Intent(Login.this, AppHome.class);//Change myLocation to AppHome
                                startActivity(intent);
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "Login Unsuccessful!", Toast.LENGTH_SHORT).show();

                                return;
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Login failed!", Toast.LENGTH_SHORT).show();

                        Log.d("Error: ", error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    private void startGPSService() {
        startService(new Intent(this, GpsCollection.class));
    }

    private void checkSessionId(String sessionId) {
        Map<String, String> params = new HashMap<String,String>();
        params.put("session_id", sessionId);

        String url = Data.getInstance().getUrl() + "/verifySession";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // still need a check here
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Intent intent = new Intent(Login.this, AppHome.class);//Change myLocation to AppHome
                                startActivity(intent);
                            }
                            else {
                                return;
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
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
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    public void forgotPassword(View view) {
        startActivity(new Intent(Login.this, ForgotPassword.class));
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