package me.vvander.wander;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
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

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final String SP_LOCATION = "locationSwitch";
    private static final String SP_SCHEDULE = "locationSchedule";
    Switch locationSwitch;
    TextView radiusText;
    SeekBar seekBar;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button changeEmailButton = findViewById(R.id.changeEmail);
        Button changeUsernameButton = findViewById(R.id.changeUsername);
        Button changePasswordButton = findViewById(R.id.changePassword);

        if (Data.getInstance().getLoggedInGoogle() || Data.getInstance().getLoggedInFacebook()) {
            changeEmailButton.setVisibility(View.GONE);
            changeUsernameButton.setVisibility(View.GONE);
            changePasswordButton.setVisibility(View.GONE);
        } else if (Data.getInstance().getLoggedIn()) {
            changeEmailButton.setVisibility(View.VISIBLE);
            changeUsernameButton.setVisibility(View.VISIBLE);
            changePasswordButton.setVisibility(View.VISIBLE);
        }

        locationSwitch = findViewById(R.id.tracking);
        locationSwitch.setChecked(Data.getInstance().getManualLocationSwitch());

        requestQueue = Volley.newRequestQueue(this);

        seekBar = findViewById(R.id.seekBar);
        radiusText = findViewById(R.id.textView);

        getCrossRadius();
        initializeSeekBar();
    }

    public void locationToggle(View view) {
        boolean value = locationSwitch.isChecked();
        Data.getInstance().setManualLocationSwitch(value);
        SharedPreferences sharedPreferences = getSharedPreferences(SP_LOCATION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("manualLocationSwitch", value);
        editor.apply();
        if (Data.getInstance().getManualLocationSwitch() && Data.getInstance().getScheduleLocationSwitch() && Data.getInstance().getActivityRecognitionLocationSwitch()) {
            startService(new Intent(getApplicationContext(), LocationCollectionService.class));
        } else {
            stopService(new Intent(getApplicationContext(), LocationCollectionService.class));
        }
    }

    public void delete(View view) {
        startActivity(new Intent(SettingsActivity.this, DeleteActivity.class));
        finish();
    }

    public void logout(View view) {
        attemptLogout();
    }

    public void changeEmail(View view) {
        if (Data.getInstance().getLoggedIn() && !Data.getInstance().getLoggedInGoogle() && !Data.getInstance().getLoggedInFacebook()) {
            startActivity(new Intent(SettingsActivity.this, ChangeEmailActivity.class));
            finish();
        }
    }

    public void changePassword(View view) {
        if (Data.getInstance().getLoggedIn() && !Data.getInstance().getLoggedInGoogle() && !Data.getInstance().getLoggedInFacebook()) {
            startActivity(new Intent(SettingsActivity.this, ChangePasswordActivity.class));
            finish();
        }
    }

    public void changeUsername(View view) {
        if (Data.getInstance().getLoggedIn() && !Data.getInstance().getLoggedInGoogle() && !Data.getInstance().getLoggedInFacebook()) {
            startActivity(new Intent(SettingsActivity.this, ChangeUsernameActivity.class));
            finish();
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
                                stopService(new Intent(getApplicationContext(), LocationCollectionService.class));
                                resetManualLocationSwitch();
                                resetScheduleLocationSwitch();
                                cancelLocationScheduleAlarm();
                                Data.getInstance().logout();
                                Data.getInstance().loginGoogle();
                                Data.getInstance().loginFacebook();
                                Data.getInstance().removeAllCookies();
                                Toast.makeText(getApplicationContext(), "Logged out!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
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


    public void setSchedule(View view) {
        startActivity(new Intent(SettingsActivity.this, LocationScheduleActivity.class));
        finish();
    }

    private void resetManualLocationSwitch() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_LOCATION, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        Data.getInstance().setManualLocationSwitch(true);
    }

    private void resetScheduleLocationSwitch() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_SCHEDULE, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        Data.getInstance().setScheduleLocationSwitch(true);
    }

    private void cancelLocationScheduleAlarm() {
        Intent intent = new Intent(getApplicationContext(), LocationScheduleAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        pendingIntent.cancel();
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void initializeSeekBar() {
        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String text = "Cross Radius: " + progress + " feet";
                radiusText.setText(text);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateCrossRadius(seekBar.getProgress());
            }
        };
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
    }

    private void updateCrossRadius(int radius) {
        String url = Data.getInstance().getUrl() + "/changeCrossRadius";
        Map<String, String> params = new HashMap<>();
        params.put("newCrossRadius", String.valueOf(radius));

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Radius updated!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Error setting radius!", Toast.LENGTH_SHORT).show();
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

    public void customize(View view){
        startActivity(new Intent(SettingsActivity.this, CustomizeAppActivity.class));
    }

    private void getCrossRadius() {
        String url = Data.getInstance().getUrl() + "/getCrossRadius";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String r = response.getString("crossRadius");
                            if (r != null) {
                                try {
                                    String text = "Cross Radius: " + r + " feet";
                                    radiusText.setText(text);
                                    seekBar.setProgress(Integer.parseInt(r));
                                } catch (NumberFormatException e) {
                                    String text = "Cross Radius: 150 feet";
                                    radiusText.setText(text);
                                    seekBar.setProgress(150);
                                }
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Error getting radius!", Toast.LENGTH_SHORT).show();
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
}