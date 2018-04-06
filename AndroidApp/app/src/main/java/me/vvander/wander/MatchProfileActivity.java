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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_profile);
        requestQueue = Volley.newRequestQueue(this);

        ImageView picture = findViewById(R.id.picture);
        TextView name = findViewById(R.id.name);
        TextView about = findViewById(R.id.about);
        TextView interests = findViewById(R.id.interests);
        TextView timesCrossed = findViewById(R.id.timesCrossed);

        final String uid = getIntent().getStringExtra("uid");
        name.setText(getIntent().getStringExtra("name"));
        about.setText(getIntent().getStringExtra("about"));
        interests.setText(getIntent().getStringExtra("interests"));
        picture.setImageBitmap(Utilities.decodeImage(getIntent().getStringExtra("picture")));
        timesCrossed.setText(getIntent().getStringExtra("timesCrossed"));
        boolean approved = getIntent().getBooleanExtra("approved", false);

        approveButton = findViewById(R.id.approveButton);
        unapproveButton = findViewById(R.id.unapproveButton);
        crossedPathsButton = findViewById(R.id.crossedPathsButton);

        approveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                approveUser(uid);
            }
        });

        unapproveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unapproveUser(uid);
            }
        });

        crossedPathsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCrossLocations(uid);
            }
        });

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

    private void approveUser(String uid) {
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

    private void unapproveUser(String uid) {
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

    private void getCrossLocations(String uid) {
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
                                    String latitude = String.valueOf(object.get("latitude"));
                                    String longitude = String.valueOf(object.get("longitude"));
                                    if (latitude != null && longitude != null && !latitude.equals("null") && !longitude.equals("null")) {
                                        LatLng point = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                                        crossList.add(point);
                                    }
                                }
                            }
                            Gson gson = new Gson();
                            String json = gson.toJson(crossList);
                            Intent intent = new Intent(MatchProfileActivity.this, MapActivity.class);
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