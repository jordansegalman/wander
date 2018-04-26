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

public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = ForgotPasswordActivity.class.getSimpleName();
    private EditText emailEdit;
    private EditText usernameEdit;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        requestQueue = Volley.newRequestQueue(this);

        emailEdit = findViewById(R.id.email);
        usernameEdit = findViewById(R.id.username);
    }

    public void done(View view) {
        String username = usernameEdit.getText().toString();
        String email = emailEdit.getText().toString();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(getApplicationContext(), "Enter a username.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Enter an email.", Toast.LENGTH_SHORT).show();
        } else {
            sendPOSTRequest(username, email);
        }
    }

    private void sendPOSTRequest(String username, String email) {
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("email", email);

        String url = Data.getInstance().getUrl() + "/forgotPassword";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
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
                                    case "Invalid email":
                                        Toast.makeText(getApplicationContext(), "Invalid email.", Toast.LENGTH_LONG).show();
                                        break;
                                    case "Invalid username":
                                        Toast.makeText(getApplicationContext(), "Invalid username.", Toast.LENGTH_LONG).show();
                                        break;
                                    case "No account found":
                                        Toast.makeText(getApplicationContext(), "No account found.", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(getApplicationContext(), "Password reset failed!", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "Password reset failed!", Toast.LENGTH_SHORT).show();
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