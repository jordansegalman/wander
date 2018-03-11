package com.example.kyle.wander;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FacebookLogin extends AppCompatActivity {
    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facebook_login);

        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        sendPOSTRequest(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        startActivity(new Intent(FacebookLogin.this, Login.class));
                    }

                    @Override
                    public void onError(FacebookException error) {
                        startActivity(new Intent(FacebookLogin.this, Login.class));
                    }
                });

        LoginManager.getInstance().logInWithReadPermissions(FacebookLogin.this, Arrays.asList("email", "public_profile"));
    }

    private void  sendPOSTRequest(AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Map<String, String> params = new HashMap<String, String>();

                        try {
                            String email = object.getString("email");
                            Log.e("EMAIL", "User email is: " + email);

                            Toast.makeText(getApplicationContext(), "Login Successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(FacebookLogin.this, AppHome.class);
                            startActivity(intent);

                        } catch (JSONException e) {
                            startActivity(new Intent(FacebookLogin.this, Login.class));
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoginManager.getInstance().logOut();
    }
}