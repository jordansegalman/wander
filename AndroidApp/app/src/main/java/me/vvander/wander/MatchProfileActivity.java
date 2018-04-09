package me.vvander.wander;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MatchProfileActivity extends AppCompatActivity {
    private static final String TAG = MatchProfileActivity.class.getSimpleName();
    private RequestQueue requestQueue;
    private Button approveButton;
    private Button unapproveButton;
    private Button crossedPathsButton;
    private String uid;
    private String name;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_profile);
        requestQueue = Volley.newRequestQueue(this);

        ImageView picture = findViewById(R.id.picture);
        TextView name = findViewById(R.id.name);
        TextView about = findViewById(R.id.about);
        TextView interests = findViewById(R.id.interests);
        TextView timesCrossed = findViewById(R.id.timesCrossed);

        uid = getIntent().getStringExtra("uid");
        this.name = getIntent().getStringExtra("name");
        name.setText(this.name);
        about.setText(getIntent().getStringExtra("about"));
        interests.setText(getIntent().getStringExtra("interests"));
        picture.setImageBitmap(Utilities.decodeImage(getIntent().getStringExtra("picture")));
        timesCrossed.setText(getIntent().getStringExtra("timesCrossed"));
        boolean approved = getIntent().getBooleanExtra("approved", false);

        approveButton = findViewById(R.id.approveButton);
        unapproveButton = findViewById(R.id.unapproveButton);
        crossedPathsButton = findViewById(R.id.crossedPathsButton);

        if (approved) {
            approveButton.setVisibility(View.GONE);
            unapproveButton.setVisibility(View.VISIBLE);
            crossedPathsButton.setEnabled(true);
        } else {
            approveButton.setVisibility(View.VISIBLE);
            unapproveButton.setVisibility(View.GONE);
            crossedPathsButton.setEnabled(false);
        }
    }

    public void chat(View view) {
        Intent intent = new Intent(MatchProfileActivity.this, ChatActivity.class);
        intent.putExtra("UID", uid);
        intent.putExtra("name", name);
        startActivity(intent);
    }

    public void approveUser(View view) {
        String url = Data.getInstance().getUrl() + "/approveUser";
        java.util.Map<String, String> params = new HashMap<>();
        params.put("uid", uid);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                approveButton.setVisibility(View.GONE);
                                unapproveButton.setVisibility(View.VISIBLE);
                                crossedPathsButton.setEnabled(true);
                                Toast.makeText(getApplicationContext(), "User approved.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
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

    public void unapproveUser(View view) {
        String url = Data.getInstance().getUrl() + "/unapproveUser";
        java.util.Map<String, String> params = new HashMap<>();
        params.put("uid", uid);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                approveButton.setVisibility(View.VISIBLE);
                                unapproveButton.setVisibility(View.GONE);
                                crossedPathsButton.setEnabled(false);
                                Toast.makeText(getApplicationContext(), "User unapproved.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
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

    public void getCrossLocations(View view) {
        String url = Data.getInstance().getUrl() + "/getCrossLocations";
        java.util.Map<String, String> params = new HashMap<>();
        params.put("uid", uid);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("Cross Locations");
                            int length = array.length();
                            ArrayList<LatLng> crossList = new ArrayList<>();
                            for (int i = 0; i < length; i++) {
                                JSONObject object = array.getJSONObject(i);
                                if (object.get("latitude") != null && object.get("longitude") != null) {
                                    double latitude = object.getDouble("latitude");
                                    double longitude = object.getDouble("longitude");
                                    crossList.add(new LatLng(latitude, longitude));
                                }
                            }
                            Gson gson = new Gson();
                            String json = gson.toJson(crossList);
                            Intent intent = new Intent(MatchProfileActivity.this, HomeActivity.class);
                            intent.putExtra("Cross List", json);
                            startActivity(intent);
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Not approved yet.", Toast.LENGTH_SHORT).show();
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