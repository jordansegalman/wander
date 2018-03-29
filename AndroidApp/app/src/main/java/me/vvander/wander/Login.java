package me.vvander.wander;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {
    private static final String TAG = Login.class.getSimpleName();
    private static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 9999;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1111;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        requestQueue = Volley.newRequestQueue(this);

        checkGooglePlayServices();

        Data.getInstance().initializeCookies(getApplicationContext());
        Data.getInstance().initializeFirebaseRegistrationToken();
        Data.getInstance().initializeLocationSchedule();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            checkSession();
        }
    }

    private void checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                Dialog errorDialog = googleApiAvailability.getErrorDialog(this, status, GOOGLE_PLAY_SERVICES_REQUEST_CODE);
                errorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                });
                errorDialog.show();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                checkSession();
            } else {
                checkSession();
            }
        }
    }

    public void login(View view) {
        EditText usernameText = findViewById(R.id.username);
        EditText passwordText = findViewById(R.id.password);
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
        if (username.length() == 0 || password.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please enter a username and password.", Toast.LENGTH_SHORT).show();
            return;
        }
        attemptLogin(username, password);
    }

    private void checkSession() {
        String url = Data.getInstance().getUrl() + "/verifySession";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Data.getInstance().login();
                                sendFirebaseRegistrationTokenToServer();
                                startLocationCollectionService();
                                Intent intent = new Intent(Login.this, Home.class);
                                startActivity(intent);
                            } else if (res.equalsIgnoreCase("google")) {
                                Data.getInstance().loginGoogle();
                                sendFirebaseRegistrationTokenToServer();
                                startLocationCollectionService();
                                Intent intent = new Intent(Login.this, Home.class);
                                startActivity(intent);
                            } else if (res.equalsIgnoreCase("facebook")) {
                                Data.getInstance().loginFacebook();
                                sendFirebaseRegistrationTokenToServer();
                                startLocationCollectionService();
                                Intent intent = new Intent(Login.this, Home.class);
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

    private void attemptLogin(String username, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);

        String url = Data.getInstance().getUrl() + "/login";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                                Data.getInstance().login();
                                sendFirebaseRegistrationTokenToServer();
                                startLocationCollectionService();
                                Intent intent = new Intent(Login.this, Home.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(getApplicationContext(), "Login failed!", Toast.LENGTH_SHORT).show();
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

    private void sendFirebaseRegistrationTokenToServer() {
        if ((Data.getInstance().getLoggedIn() || Data.getInstance().getLoggedInGoogle() || Data.getInstance().getLoggedInFacebook()) && Data.getInstance().getFirebaseRegistrationToken() != null) {
            Map<String, String> params = new HashMap<>();
            params.put("firebaseRegistrationToken", Data.getInstance().getFirebaseRegistrationToken());

            String url = Data.getInstance().getUrl() + "/addFirebaseRegistrationToken";

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String res = response.getString("response");
                                if (res.equalsIgnoreCase("pass")) {
                                    Log.d(TAG, "Firebase registration token successfully added to server.");
                                } else {
                                    Log.d(TAG, "Error adding Firebase registration token to server.");
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
    }

    private void startLocationCollectionService() {
        if (Data.getInstance().getManualLocationSwitch() && Data.getInstance().getScheduleLocationSwitch() && Data.getInstance().getActivityRecognitionLocationSwitch()) {
            startService(new Intent(getApplicationContext(), LocationCollectionService.class));
        } else {
            stopService(new Intent(getApplicationContext(), LocationCollectionService.class));
        }
    }

    public void forgotPassword(View view) {
        startActivity(new Intent(Login.this, ForgotPassword.class));
    }

    public void facebookButton(View view) {
        startActivity(new Intent(Login.this, FacebookLogin.class));
    }

    public void googleButton(View view) {
        startActivity(new Intent(Login.this, GoogleLogin.class));
    }

    public void registerButton(View view) {
        startActivity(new Intent(Login.this, Registration.class));
    }
}