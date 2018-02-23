package com.example.kyle.wander;

import android.content.Intent;
import android.support.design.widget.Snackbar;
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

public class Delete extends AppCompatActivity {
    private RequestQueue requestQueue;
    EditText passwordEdit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete);
        requestQueue = Volley.newRequestQueue(this);
        passwordEdit = (EditText)findViewById(R.id.password);


    }

    public void delete(View view){
        //TODO: delete account
        if (passwordEdit.getText().toString().length() == 0) {
            Snackbar.make(findViewById(R.id.myCoordinatorLayout),
                    "Please enter your password", Snackbar.LENGTH_LONG).show();
            return;
        }
        sendPOSTRequest(passwordEdit.getText().toString());
        //startActivity(new Intent(Delete.this, Login.class));
    }

    private void sendPOSTRequest(String password) {
        String username = Data.getInstance().getUsername();
        String email = Data.getInstance().getEmail();

        Map<String, String> params = new HashMap<String,String>();
        params.put("username", username);
        params.put("password", password);
        params.put("email", email);

        String url = Data.getInstance().getUrl() + "/deleteAccount";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Accounted deleted!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(Delete.this, Login.class);//Change myLocation to AppHome
                                startActivity(intent);
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "Account deletion failed!", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(getApplicationContext(), "Account deletion failed!", Toast.LENGTH_SHORT).show();

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

    public void back(View view){
        startActivity(new Intent(Delete.this, Settings.class));
    }
}
