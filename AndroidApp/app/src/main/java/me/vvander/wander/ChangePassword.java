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

public class ChangePassword extends AppCompatActivity {
    private RequestQueue requestQueue;

    EditText oldPasswordEdit;
    EditText passwordEdit;
    EditText confirmEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        passwordEdit = (EditText)findViewById(R.id.password);
        oldPasswordEdit = (EditText)findViewById(R.id.old_password);
        confirmEdit = (EditText)findViewById(R.id.confirm);
        requestQueue = Volley.newRequestQueue(this);
    }

    public void done(View view){
        String password = passwordEdit.getText().toString();
        String confirmPassword = confirmEdit.getText().toString();
        String oldPassword = oldPasswordEdit.getText().toString();


        if(!password.equals(confirmPassword)){
            Toast.makeText(getApplicationContext(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
        } else {
            sendPOSTRequest(oldPassword, password);
            //startActivity(new Intent(ChangePassword.this, Settings.class));
        }
    }

    private void sendPOSTRequest(String password, String newPassword) {
        Map<String, String> params = new HashMap<String,String>();
        params.put("password", password);
        params.put("newPassword", newPassword);

        String url = Data.getInstance().getUrl() + "/changePassword";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Password changed!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ChangePassword.this, Settings.class);
                                startActivity(intent);
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "Password change failed!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Password change failed!", Toast.LENGTH_SHORT).show();

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