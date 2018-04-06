package me.vvander.wander;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Space;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Matches extends AppCompatActivity {
    private static final String TAG = Matches.class.getSimpleName();
    private RequestQueue requestQueue;
    private ArrayList<MatchData> matchList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matches);
        requestQueue = Volley.newRequestQueue(this);
        matchList = new ArrayList<>();
        requestAllMatches();
    }

    private void requestAllMatches() {
        String url = Data.getInstance().getUrl() + "/getAllMatches";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("UIDs");
                            int length = array.length();
                            if (length == 0) {
                                setupListView();
                            } else {
                                for (int i = 0; i < length; i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String uid = String.valueOf(object.get("uid"));
                                    requestSingleMatch(uid);
                                }
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

    private void requestSingleMatch(String uid) {
        String url = Data.getInstance().getUrl() + "/getMatch";
        Map<String, String> params = new HashMap<>();
        params.put("uid", uid);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("Profile");
                            JSONObject object = array.getJSONObject(0);
                            String uid = String.valueOf(object.get("uid"));
                            String name = String.valueOf(object.get("name"));
                            String about = String.valueOf(object.get("about"));
                            String interests = String.valueOf(object.get("interests"));
                            String picture = String.valueOf(object.get("picture"));
                            int timesCrossed = Integer.parseInt(String.valueOf(object.get("timesCrossed")));
                            boolean approved = Boolean.valueOf(String.valueOf(object.get("approved")));

                            if (picture == null || !picture.equalsIgnoreCase("null")) {
                                picture = Utilities.encodeImage(BitmapFactory.decodeResource(getResources(), R.drawable.default_profile));
                            }

                            MatchData match = new MatchData(uid, name, about, interests, picture, timesCrossed, approved);
                            matchList.add(match);
                            setupListView();
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.toString());
                        setupListView();
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    private void setupListView() {
        Space noMatchesTopSpace = findViewById(R.id.noMatchesTopSpace);
        TextView noMatchesText = findViewById(R.id.noMatchesText);
        Space noMatchesBottomSpace = findViewById(R.id.noMatchesBottomSpace);
        ListView matchListView = findViewById(R.id.matchList);

        if (!matchList.isEmpty()) {
            noMatchesTopSpace.setVisibility(View.GONE);
            noMatchesText.setVisibility(View.GONE);
            noMatchesBottomSpace.setVisibility(View.GONE);
            matchListView.setVisibility(View.VISIBLE);
            ProfileAdapter profileAdapter = new ProfileAdapter(this, matchList);
            matchListView.setAdapter(profileAdapter);
            matchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final MatchData matchData = (MatchData) parent.getAdapter().getItem(position);

                    Intent intent = new Intent(Matches.this, MatchProfile.class);
                    intent.putExtra("uid", matchData.getUserID());
                    intent.putExtra("name", matchData.getName());
                    intent.putExtra("about", matchData.getAbout());
                    intent.putExtra("interests", matchData.getInterests());
                    intent.putExtra("picture", matchData.getPicture());
                    intent.putExtra("timesCrossed", String.valueOf(matchData.getTimesCrossed()));
                    intent.putExtra("approved", matchData.getApproved());
                    startActivity(intent);
                }
            });
        } else {
            noMatchesTopSpace.setVisibility(View.VISIBLE);
            noMatchesText.setVisibility(View.VISIBLE);
            noMatchesBottomSpace.setVisibility(View.VISIBLE);
            matchListView.setVisibility(View.GONE);
            matchListView.setEnabled(false);
        }
    }
}