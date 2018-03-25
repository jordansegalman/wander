package me.vvander.wander;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
    private static final String TAG = Settings.class.getSimpleName();
    private static final String SP_LOCATION = "locationSwitch";
    Switch locationSwitch;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        locationSwitch = findViewById(R.id.tracking);
        locationSwitch.setChecked(Data.getInstance().getManualLocationSwitch());

        requestQueue = Volley.newRequestQueue(this);
    }

    public void locationToggle(View view) {
        boolean value = locationSwitch.isChecked();
        Data.getInstance().setManualLocationSwitch(value);
        SharedPreferences sharedPreferences = getSharedPreferences(SP_LOCATION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("manualLocationSwitch", value);
        editor.apply();
    }

    public void delete(View view) {
        startActivity(new Intent(Settings.this, Delete.class));
    }

    public void logout(View view) {
        attemptLogout();
    }

    public void changeEmail(View view) {
        if (Data.getInstance().getLoggedIn() && !Data.getInstance().getLoggedInGoogle() && !Data.getInstance().getLoggedInFacebook()) {
            startActivity(new Intent(Settings.this, ChangeEmail.class));
        }
    }

    public void changePassword(View view) {
        if (Data.getInstance().getLoggedIn() && !Data.getInstance().getLoggedInGoogle() && !Data.getInstance().getLoggedInFacebook()) {
            startActivity(new Intent(Settings.this, ChangePassword.class));
        }
    }

    public void changeUsername(View view) {
        if (Data.getInstance().getLoggedIn() && !Data.getInstance().getLoggedInGoogle() && !Data.getInstance().getLoggedInFacebook()) {
            startActivity(new Intent(Settings.this, ChangeUsername.class));
        }
    }

    private void attemptLogout() {
        String url = Data.getInstance().getUrl() + "/logout";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Logged out!", Toast.LENGTH_SHORT).show();
                                resetManualLocationSwitch();
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
                        Log.d(TAG, error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    private void resetManualLocationSwitch() {
        Data.getInstance().setManualLocationSwitch(true);
        SharedPreferences sharedPreferences = getSharedPreferences(SP_LOCATION, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(Settings.this, AppHome.class));
    }
}