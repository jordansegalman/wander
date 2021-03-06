package me.vvander.wander;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.ActivityRecognitionClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DeleteActivity extends AppCompatActivity {
    private static final String TAG = DeleteActivity.class.getSimpleName();
    private static final String SP_LOCATION = "locationSwitch";
    private static final String SP_SCHEDULE = "locationSchedule";
    private static final String SP_NOTIFICATIONS = "notificationSwitch";
    private static final String SP_THEME = "theme";
    private EditText passwordEditText;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete);

        Space passwordSpace = findViewById(R.id.passwordSpace);
        TextView passwordTextView = findViewById(R.id.passwordTextView);
        passwordEditText = findViewById(R.id.passwordEditText);

        if (Data.getInstance().getLoggedInGoogle() || Data.getInstance().getLoggedInFacebook()) {
            passwordSpace.setVisibility(View.GONE);
            passwordTextView.setVisibility(View.GONE);
            passwordEditText.setVisibility(View.GONE);
        } else if (Data.getInstance().getLoggedIn()) {
            passwordSpace.setVisibility(View.VISIBLE);
            passwordTextView.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.VISIBLE);
        }

        requestQueue = Volley.newRequestQueue(this);
    }

    public void delete(View view) {
        String password = passwordEditText.getText().toString();

        if (Data.getInstance().getLoggedIn() && !Data.getInstance().getLoggedInGoogle() && !Data.getInstance().getLoggedInFacebook()) {
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getApplicationContext(), "Enter your password.", Toast.LENGTH_SHORT).show();
            } else {
                attemptDelete(password);
            }
        } else if (!Data.getInstance().getLoggedIn() && (Data.getInstance().getLoggedInGoogle() || Data.getInstance().getLoggedInFacebook())) {
            attemptDelete(null);
        }
    }

    private void attemptDelete(String password) {
        if (Data.getInstance().getLoggedIn()) {
            Map<String, String> params = new HashMap<>();
            params.put("password", password);

            String url = Data.getInstance().getUrl() + "/deleteAccount";

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String res = response.getString("response");
                                if (res.equalsIgnoreCase("pass")) {
                                    stopService(new Intent(getApplicationContext(), LocationCollectionService.class));
                                    resetManualLocationSwitch();
                                    resetScheduleLocationSwitch();
                                    resetActivityRecognitionLocationSwitch();
                                    resetNotificationSwitch();
                                    cancelLocationScheduleAlarm();
                                    cancelActivityRecognition();
                                    resetTheme();
                                    cancelNotifications();
                                    Data.getInstance().logout();
                                    Data.getInstance().removeAllCookies();
                                    Toast.makeText(getApplicationContext(), "Account deleted!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(DeleteActivity.this, LoginActivity.class);
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
                            if (error.networkResponse.data != null) {
                                try {
                                    String res = new JSONObject(new String(error.networkResponse.data)).getString("response");
                                    switch (res) {
                                        case "Invalid password":
                                            Toast.makeText(getApplicationContext(), "Invalid password!", Toast.LENGTH_LONG).show();
                                            break;
                                        default:
                                            Toast.makeText(getApplicationContext(), "Account deletion failed!", Toast.LENGTH_SHORT).show();
                                            break;
                                    }
                                } catch (JSONException e) {
                                    Toast.makeText(getApplicationContext(), "Account deletion failed!", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            }
                            Log.d(TAG, error.toString());
                        }
                    }
            );
            postRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(postRequest);
        } else if (Data.getInstance().getLoggedInGoogle()) {
            String url = Data.getInstance().getUrl() + "/googleDeleteAccount";

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
                                    resetActivityRecognitionLocationSwitch();
                                    resetNotificationSwitch();
                                    cancelLocationScheduleAlarm();
                                    cancelActivityRecognition();
                                    resetTheme();
                                    cancelNotifications();
                                    Data.getInstance().logoutGoogle();
                                    Data.getInstance().removeAllCookies();
                                    Toast.makeText(getApplicationContext(), "Account deleted!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(DeleteActivity.this, LoginActivity.class);
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
                            Toast.makeText(getApplicationContext(), "Account deletion failed!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, error.toString());
                        }
                    }
            );
            postRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(postRequest);
        } else if (Data.getInstance().getLoggedInFacebook()) {
            String url = Data.getInstance().getUrl() + "/facebookDeleteAccount";

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
                                    resetActivityRecognitionLocationSwitch();
                                    resetNotificationSwitch();
                                    cancelLocationScheduleAlarm();
                                    cancelActivityRecognition();
                                    resetTheme();
                                    cancelNotifications();
                                    Data.getInstance().logoutFacebook();
                                    Data.getInstance().removeAllCookies();
                                    Toast.makeText(getApplicationContext(), "Account deleted!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(DeleteActivity.this, LoginActivity.class);
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
                            Toast.makeText(getApplicationContext(), "Account deletion failed!", Toast.LENGTH_SHORT).show();
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

    private void resetActivityRecognitionLocationSwitch() {
        Data.getInstance().setActivityRecognitionLocationSwitch(false);
    }

    private void resetNotificationSwitch() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_NOTIFICATIONS, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        Data.getInstance().setNotificationSwitch(true);
    }

    private void cancelLocationScheduleAlarm() {
        Intent intent = new Intent(getApplicationContext(), LocationScheduleAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        pendingIntent.cancel();
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void cancelActivityRecognition() {
        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        pendingIntent.cancel();
        ActivityRecognitionClient activityRecognitionClient = new ActivityRecognitionClient(getApplicationContext());
        activityRecognitionClient.removeActivityUpdates(pendingIntent);
    }

    private void resetTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_THEME, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    private void cancelNotifications() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    public void cancel(View view) {
        startActivity(new Intent(DeleteActivity.this, SettingsActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(DeleteActivity.this, SettingsActivity.class));
        finish();
    }
}