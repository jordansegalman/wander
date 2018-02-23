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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

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
        //TODO: turn off location services. "value" indicates whether it should be on(true) or off(false)
        //This code is run every time the user hits the button on the settings page

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("tracking", value);
        editor.commit();

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
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fs = getBaseContext().openFileInput(Data.getInstance().getSessionInfoFile());
            InputStreamReader ir = new InputStreamReader(fs);
            BufferedReader br = new BufferedReader(ir);

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {e.printStackTrace();}

        //String username = Data.getInstance().getUsername();
        String session_id = sb.toString();
        Log.d("tag", session_id);
        //Log.d("tag", username);


        Map<String, String> params = new HashMap<String,String>();
        //params.put("username", username);
        params.put("session_id", session_id);

        String url = Data.getInstance().getUrl() + "/logout";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                try
                                {
                                    FileOutputStream fos = openFileOutput(Data.getInstance().getSessionInfoFile(), Context.MODE_PRIVATE);
                                    String s = "";
                                    fos.write(s.getBytes());
                                    fos.close();
                                } catch (IOException e) {e.printStackTrace();}


                                Toast.makeText(getApplicationContext(), "Logged Out!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(Settings.this, Login.class);//Change myLocation to AppHome
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
