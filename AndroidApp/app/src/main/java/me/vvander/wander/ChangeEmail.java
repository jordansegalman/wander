package me.vvander.wander;

import android.content.Intent;
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

public class ChangeEmail extends AppCompatActivity {
    private RequestQueue requestQueue;

    EditText emailEdit;
    EditText newEmailEdit;
    EditText passwordEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

        emailEdit = (EditText)findViewById(R.id.current_email);
        newEmailEdit = (EditText)findViewById(R.id.new_email);
        passwordEdit = (EditText)findViewById(R.id.password);
        requestQueue = Volley.newRequestQueue(this);
    }

    public void done(View view){
        String newEmail = newEmailEdit.getText().toString();
        String password = passwordEdit.getText().toString();

        sendPOSTRequest(newEmail, password);
        //startActivity(new Intent(ChangeEmail.this, Settings.class));
    }

    private void sendPOSTRequest(String newEmail, String password) {
        Map<String, String> params = new HashMap<String,String>();
        params.put("password", password);
        params.put("newEmail", newEmail);

        String url = Data.getInstance().getUrl() + "/changeEmail";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Email changed!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ChangeEmail.this, Settings.class);
                                startActivity(intent);
                            }
                            else {
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
                        Toast.makeText(getApplicationContext(), "Email change failed!", Toast.LENGTH_SHORT).show();

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
}