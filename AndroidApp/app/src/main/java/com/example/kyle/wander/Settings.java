package com.example.kyle.wander;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class Settings extends AppCompatActivity {
    Switch locationSwitch;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        boolean locationTracking = sharedPref.getBoolean("tracking", true);

        locationSwitch = (Switch) findViewById(R.id.tracking);
        locationSwitch.setChecked(locationTracking);

        requestQueue = Volley.newRequestQueue(this);
    }

    public void locationToggle(View view){
        boolean value = locationSwitch.isChecked();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("tracking", value);
        editor.apply();
        if (value) {
            startService(new Intent(this, GpsCollection.class));
        } else {
            stopService(new Intent(this, GpsCollection.class));
        }
    }

    public void delete(View view){
        startActivity(new Intent(Settings.this, Delete.class));
    }

    public void logout(View view) {
        logOut();
    }

    public void changeEmail(View view){
        startActivity(new Intent(Settings.this, ChangeEmail.class));
    }

    public void changePassword(View view){
        startActivity(new Intent(Settings.this, ChangePassword.class));
    }

    public void changeUsername(View view) {
        startActivity(new Intent(Settings.this, ChangeUsername.class));
    }

    private void logOut() {
        String url = Data.getInstance().getUrl() + "/logout";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Logged out!", Toast.LENGTH_SHORT).show();
                                Data.getInstance().logout();
                                Data.getInstance().removeAllCookies();
                                Intent intent = new Intent(Settings.this, Login.class);
                                startActivity(intent);
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
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(Settings.this, AppHome.class));
    }
}