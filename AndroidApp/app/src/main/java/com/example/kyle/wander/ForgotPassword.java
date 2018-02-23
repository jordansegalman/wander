package com.example.kyle.wander;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class ForgotPassword extends AppCompatActivity {
    private RequestQueue requestQueue;

    EditText emailEdit;
    EditText usernameEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        requestQueue = Volley.newRequestQueue(this);

        emailEdit = (EditText)findViewById(R.id.email);
        usernameEdit = (EditText)findViewById(R.id.username);
    }

    private void sendPOSTRequest(String username, String email) {

        Map<String, String> params = new HashMap<String,String>();
        params.put("username", username);
        params.put("email", email);

        String url = Data.getInstance().getUrl() + "/forgotPassword";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Password reset email sent!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(ForgotPassword.this, Login.class);//Change myLocation to AppHome
                                startActivity(intent);
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "Password reset failed!", Toast.LENGTH_LONG).show();
                                return;
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Password reset failed!", Toast.LENGTH_LONG).show();

                        Log.d("Error: ", error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    public void done(View view) {
        String email = emailEdit.getText().toString();
        String username = usernameEdit.getText().toString();
        Log.d("tag", email);
        Log.d("tag", username);
        sendPOSTRequest(username, email);
    }
}
