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

public class ChangeEmailActivity extends AppCompatActivity {
    private static final String TAG = ChangeEmailActivity.class.getSimpleName();
    private EditText emailEdit;
    private EditText passwordEdit;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

        emailEdit = findViewById(R.id.new_email);
        passwordEdit = findViewById(R.id.password);
        requestQueue = Volley.newRequestQueue(this);
    }

    public void done(View view) {
        String newEmail = emailEdit.getText().toString();
        String password = passwordEdit.getText().toString();

        if (TextUtils.isEmpty(newEmail)) {
            Toast.makeText(getApplicationContext(), "Enter a new email.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Enter your password.", Toast.LENGTH_SHORT).show();
        } else {
            sendPOSTRequest(newEmail, password);
        }
    }

    private void sendPOSTRequest(String newEmail, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("newEmail", newEmail);
        params.put("password", password);

        String url = Data.getInstance().getUrl() + "/changeEmail";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Email changed!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ChangeEmailActivity.this, SettingsActivity.class);
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
                                    case "Invalid new email":
                                        Toast.makeText(getApplicationContext(), "New email must be valid and have a minimum length of 3 characters and a maximum length of 255 characters.", Toast.LENGTH_LONG).show();
                                        break;
                                    case "Email taken":
                                        Toast.makeText(getApplicationContext(), "Email taken!", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(getApplicationContext(), "Email change failed!", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "Email change failed!", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ChangeEmailActivity.this, SettingsActivity.class));
        finish();
    }
}