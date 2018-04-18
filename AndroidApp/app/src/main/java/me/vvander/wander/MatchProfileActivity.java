package me.vvander.wander;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private static final int MAX_REASON_LENGTH = 1024;
    private RequestQueue requestQueue;
    private Button approveButton;
    private Button unapproveButton;
    private Button crossedPathsButton;
    private String uid;
    private String name;

    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Customize.getCustomTheme(this));

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.match_profile_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.unmatch:
                new AlertDialog.Builder(this)
                        .setTitle("Are you sure you want to unmatch " + name + "?")
                        .setPositiveButton("Unmatch", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                unmatch();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            case R.id.block:
                new AlertDialog.Builder(this)
                        .setTitle("Are you sure you want to block " + name + "?")
                        .setPositiveButton("Block", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                block();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            case R.id.report:
                final EditText reasonEditText = new EditText(this);
                reasonEditText.setHint("Reason");
                reasonEditText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                reasonEditText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(MAX_REASON_LENGTH)});
                new AlertDialog.Builder(this)
                        .setTitle("What is your reason for reporting " + name + "?")
                        .setView(reasonEditText)
                        .setPositiveButton("Report", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String reason = reasonEditText.getText().toString();
                                if (TextUtils.isEmpty(reason)) {
                                    Toast.makeText(getApplicationContext(), "Reason cannot be empty.", Toast.LENGTH_SHORT).show();
                                } else if (reason.length() > MAX_REASON_LENGTH) {
                                    Toast.makeText(getApplicationContext(), "Reason cannot be greater than 1024 characters.", Toast.LENGTH_SHORT).show();
                                } else {
                                    report(reason);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void unmatch() {
        String url = Data.getInstance().getUrl() + "/unmatchUser";
        java.util.Map<String, String> params = new HashMap<>();
        params.put("uid", uid);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "User unmatched.", Toast.LENGTH_SHORT).show();
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

    private void block() {
        String url = Data.getInstance().getUrl() + "/blockUser";
        java.util.Map<String, String> params = new HashMap<>();
        params.put("uid", uid);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "User blocked.", Toast.LENGTH_SHORT).show();
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

    private void report(String reason) {
        String url = Data.getInstance().getUrl() + "/reportUser";
        java.util.Map<String, String> params = new HashMap<>();
        params.put("uid", uid);
        params.put("reason", reason);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "User reported and blocked.", Toast.LENGTH_SHORT).show();
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