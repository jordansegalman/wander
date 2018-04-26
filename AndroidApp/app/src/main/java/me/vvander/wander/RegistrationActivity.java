package me.vvander.wander;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = RegistrationActivity.class.getSimpleName();
    private EditText emailText;
    private EditText usernameText;
    private EditText passwordText;
    private EditText confirmPasswordText;
    private RequestQueue requestQueue;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        emailText = findViewById(R.id.email);
        usernameText = findViewById(R.id.username);
        passwordText = findViewById(R.id.password);
        confirmPasswordText = findViewById(R.id.confirmPassword);

        requestQueue = Volley.newRequestQueue(this);
        url = Data.getInstance().getUrl() + "/registerAccount";
    }

    public void submit(View view) {
        String email = emailText.getText().toString();
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
        String confirmPassword = confirmPasswordText.getText().toString();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Enter an email.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(username)) {
            Toast.makeText(getApplicationContext(), "Enter a username.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Enter a password.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(getApplicationContext(), "Confirm your password.", Toast.LENGTH_SHORT).show();
        } else if (!password.equals(confirmPassword)) {
            Toast.makeText(getApplicationContext(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
        } else {
            sendPOSTRequest(email, username, password);
        }
    }

    private void sendPOSTRequest(String email, String username, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("email", email);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Account successfully created!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
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
                        if (error.networkResponse.data != null) {
                            try {
                                String res = new JSONObject(new String(error.networkResponse.data)).getString("response");
                                switch (res) {
                                    case "Invalid email":
                                        Toast.makeText(getApplicationContext(), "Email must be valid and have a minimum length of 3 characters and a maximum length of 255 characters.", Toast.LENGTH_LONG).show();
                                        break;
                                    case "Invalid username":
                                        Toast.makeText(getApplicationContext(), "Username must be alphanumeric and have a minimum length of 4 characters and a maximum length of 24 characters.", Toast.LENGTH_LONG).show();
                                        break;
                                    case "Invalid password":
                                        Toast.makeText(getApplicationContext(), "Password must only contain ASCII characters and must have a minimum length of 8 characters and a maximum length of 64 characters.", Toast.LENGTH_LONG).show();
                                        break;
                                    case "Email taken":
                                        Toast.makeText(getApplicationContext(), "Email taken!", Toast.LENGTH_LONG).show();
                                        break;
                                    case "Username taken":
                                        Toast.makeText(getApplicationContext(), "Username taken!", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(getApplicationContext(), "Registration failed!", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "Registration failed!", Toast.LENGTH_SHORT).show();
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
    }
}