package me.vvander.wander;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
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
    EditText emailEdit;
    EditText passwordEdit;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Customize.getCustomTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

        emailEdit = findViewById(R.id.new_email);
        passwordEdit = findViewById(R.id.password);
        requestQueue = Volley.newRequestQueue(this);
    }

    public void done(View view) {
        String newEmail = emailEdit.getText().toString();
        String password = passwordEdit.getText().toString();

        sendPOSTRequest(newEmail, password);
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
                            } else {
                                Toast.makeText(getApplicationContext(), "Email change failed!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Toast.makeText(getApplicationContext(), "Email change failed!", Toast.LENGTH_SHORT).show();
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null) {
                            Toast.makeText(getApplicationContext(), new String(networkResponse.data), Toast.LENGTH_LONG).show();
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