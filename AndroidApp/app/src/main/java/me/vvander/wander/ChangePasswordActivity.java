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

public class ChangePasswordActivity extends AppCompatActivity {
    private static final String TAG = ChangePasswordActivity.class.getSimpleName();
    private EditText oldPasswordEdit;
    private EditText newPasswordEdit;
    private EditText confirmPasswordEdit;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        oldPasswordEdit = findViewById(R.id.old_password);
        newPasswordEdit = findViewById(R.id.password);
        confirmPasswordEdit = findViewById(R.id.confirm);
        requestQueue = Volley.newRequestQueue(this);
    }

    public void done(View view) {
        String oldPassword = oldPasswordEdit.getText().toString();
        String newPassword = newPasswordEdit.getText().toString();
        String confirmPassword = confirmPasswordEdit.getText().toString();

        if (TextUtils.isEmpty(oldPassword)) {
            Toast.makeText(getApplicationContext(), "Enter your password.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(getApplicationContext(), "Enter a new password.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(getApplicationContext(), "Confirm your new password.", Toast.LENGTH_SHORT).show();
        } else if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getApplicationContext(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
        } else {
            sendPOSTRequest(newPassword, oldPassword);
        }
    }

    private void sendPOSTRequest(String newPassword, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("newPassword", newPassword);
        params.put("password", password);

        String url = Data.getInstance().getUrl() + "/changePassword";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Password changed!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ChangePasswordActivity.this, SettingsActivity.class);
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
                                    case "Invalid new password":
                                        Toast.makeText(getApplicationContext(), "New password must only contain ASCII characters and must have a minimum length of 8 characters and a maximum length of 64 characters.", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(getApplicationContext(), "Password change failed!", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "Password change failed!", Toast.LENGTH_SHORT).show();
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
        startActivity(new Intent(ChangePasswordActivity.this, SettingsActivity.class));
        finish();
    }
}