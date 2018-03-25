package me.vvander.wander;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Delete extends AppCompatActivity {
    private static final String TAG = Delete.class.getSimpleName();
    private static final String SP_LOCATION = "locationSwitch";
    EditText passwordEdit;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete);
        requestQueue = Volley.newRequestQueue(this);
        passwordEdit = findViewById(R.id.password);
    }

    public void delete(View view) {
        if (Data.getInstance().getLoggedIn() && !Data.getInstance().getLoggedInGoogle() && !Data.getInstance().getLoggedInFacebook()) {
            if (passwordEdit.getText().toString().length() == 0) {
                Toast.makeText(getApplicationContext(), "Please enter your password.", Toast.LENGTH_SHORT).show();
                return;
            }
            sendPOSTRequest(passwordEdit.getText().toString());
        } else if (!Data.getInstance().getLoggedIn() && (Data.getInstance().getLoggedInGoogle() || Data.getInstance().getLoggedInFacebook())) {
            sendPOSTRequest(null);
        }
    }

    private void sendPOSTRequest(String password) {
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
                                    Toast.makeText(getApplicationContext(), "Account deleted!", Toast.LENGTH_SHORT).show();
                                    resetManualLocationSwitch();
                                    Data.getInstance().logout();
                                    Data.getInstance().removeAllCookies();
                                    Intent intent = new Intent(Delete.this, Login.class);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(getApplicationContext(), "Account deletion failed!", Toast.LENGTH_SHORT).show();
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
        } else if (Data.getInstance().getLoggedInGoogle()) {
            String url = Data.getInstance().getUrl() + "/googleDeleteAccount";

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String res = response.getString("response");
                                if (res.equalsIgnoreCase("pass")) {
                                    Toast.makeText(getApplicationContext(), "Account deleted!", Toast.LENGTH_SHORT).show();
                                    resetManualLocationSwitch();
                                    Data.getInstance().logoutGoogle();
                                    Data.getInstance().removeAllCookies();
                                    Intent intent = new Intent(Delete.this, Login.class);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(getApplicationContext(), "Account deletion failed!", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(getApplicationContext(), "Account deleted!", Toast.LENGTH_SHORT).show();
                                    resetManualLocationSwitch();
                                    Data.getInstance().logoutFacebook();
                                    Data.getInstance().removeAllCookies();
                                    Intent intent = new Intent(Delete.this, Login.class);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(getApplicationContext(), "Account deletion failed!", Toast.LENGTH_SHORT).show();
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
        Data.getInstance().setManualLocationSwitch(true);
        SharedPreferences sharedPreferences = getSharedPreferences(SP_LOCATION, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    public void back(View view) {
        startActivity(new Intent(Delete.this, Settings.class));
    }
}