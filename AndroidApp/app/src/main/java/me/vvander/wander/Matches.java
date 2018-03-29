package me.vvander.wander;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class Matches extends AppCompatActivity {
    ArrayList<MatchData> matchList;
    Map<String, MatchData> matchMap;
    ListView matchListView;
    ProfileAdapter adapter;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matches);
        requestQueue = Volley.newRequestQueue(this);
        matchMap = new HashMap<>();
        matchList = new ArrayList<>();
        requestAllMatches();
    }

    private void populateList() {
        //TODO: get matches information and put it in matchList
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
                            for (int i = 0; i < length; i++) {
                                JSONObject object = array.getJSONObject(i);
                                String uid = String.valueOf(object.get("uid"));
                                requestSingleMatch(uid);
                            }

                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG", error.toString());
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
                            // Should only be one item in the array
                            JSONArray array = response.getJSONArray("Profile");

                            JSONObject object = array.getJSONObject(0);
                            String uid = String.valueOf(object.get("uid"));
                            Log.d("UID", uid);
                            String picture = String.valueOf(object.get("picture"));
                            Log.d("Picture", picture);
                            String about = String.valueOf(object.get("about"));
                            Log.d("About", about);
                            String interests = String.valueOf(object.get("interests"));
                            Log.d("Interests", interests);
                            String firstName = String.valueOf(object.get("firstName"));
                            Log.d("Name", firstName);
                            int timesCrossed = Integer.parseInt(String.valueOf(object.get("timesCrossed")));
                            Log.d("Times Crossed", timesCrossed + "");
                            String approved = String.valueOf(object.get("approved"));
                            Log.d("Approved", approved);
                            String otherApproved = String.valueOf(object.get("otherApproved"));
                            Log.d("Other Approved", otherApproved);


                            MatchData match = new MatchData();
                            match.setAbout(about);
                            match.setUserId(uid);
                            match.setInterests(interests);
                            match.setPicture(picture);
                            match.setName(firstName);
                            match.setNumPathCrosses(timesCrossed);
                            match.setApproved(Boolean.valueOf(approved));
                            Log.d("All profile information", uid + " " + about + " " + interests);
                            matchMap.put(uid, match);
                            setupListView();

                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG", error.toString());
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

    private void approveUser(String uid) {
        String url = Data.getInstance().getUrl() + "/approveUser";
        Map<String, String> params = new HashMap<>();
        params.put("uid", uid);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
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
                        Log.d("TAG", error.toString());
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
        Map<String, String> params = new HashMap<>();
        params.put("uid", uid);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
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
                        Log.d("TAG", error.toString());
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
        Map<String, String> params = new HashMap<>();
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
                            Intent intent = new Intent(Matches.this, Map.class);
                            intent.putExtra("Cross List", json);
                            ActivityCompat.startActivityForResult(Matches.this, intent, 0, null);
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG", error.toString());
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
        if (adapter != null) {
            adapter.clear();
        }

        if (matchMap == null || matchMap.isEmpty()) {
            MatchData matchData = new MatchData();
            matchData.setName("No New Matches");
            matchList.add(matchData);
        } else {
            for (int i = 0; i < matchMap.size(); i++) {
                //listItems[i] = matchMap.get(i).getName();
                Iterator it = matchMap.entrySet().iterator();
                while (it.hasNext()) {
                    java.util.Map.Entry pair = (java.util.Map.Entry) it.next();
                    MatchData matchData = (MatchData) pair.getValue();
                    //listItems[i] = matchData.getName();
                    matchList.add(matchData);
                    it.remove();
                }
            }
        }


        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_1, matchList);
        //ArrayList<MatchData> arrayList = new ArrayList<MatchData>();
        adapter = new ProfileAdapter(this, matchList);

        matchListView = findViewById(R.id.matchesList);
        matchListView.setAdapter(adapter);

        matchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*TODO: when a user clicks on a match in the list this is triggered. The value of position is equal
                to the index of the corresponding matchData in matchList*/
                //int position = (Integer) view.getTag();
                // Access the row position here to get the correct data item
                Log.d("This is the match data", "Test");
                final MatchData matchData = (MatchData) parent.getAdapter().getItem(position);
                Log.d("This is the match data", matchData.getName());
                Log.d("This is the match data", matchData.getAbout());
                Log.d("This is the match data", matchData.getInterests());
                Log.d("This is the match data", matchData.getPicture());

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(Matches.this);
                View mView = getLayoutInflater().inflate(R.layout.match_profile, null);

                TextView name = mView.findViewById(R.id.name);
                EditText interests = mView.findViewById(R.id.interests_text);
                EditText about = mView.findViewById(R.id.about_text);
                EditText timesCrossed = mView.findViewById(R.id.crossed_text);
                ImageView picture = mView.findViewById(R.id.picture);

                final Button approve = mView.findViewById(R.id.approveButton);
                if (matchData.getApproved()) {
                    approve.setText("Unapprove");
                    approve.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (approve.getText().equals("Approve")) {
                                approveUser(matchData.getUserId());
                                approve.setText("Unapprove");
                                matchData.setApproved(true);
                            } else if (approve.getText().equals("Unapprove")) {
                                unapproveUser(matchData.getUserId());
                                approve.setText("Approve");
                                matchData.setApproved(false);
                            }
                        }
                    });
                } else {
                    approve.setText("Approve");
                    approve.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (approve.getText().equals("Approve")) {
                                approveUser(matchData.getUserId());
                                approve.setText("Unapprove");
                                matchData.setApproved(true);
                            } else if (approve.getText().equals("Unapprove")) {
                                unapproveUser(matchData.getUserId());
                                approve.setText("Approve");
                                matchData.setApproved(false);
                            }
                        }
                    });
                }
                Button crossed = mView.findViewById(R.id.crossedPathsButton);


                crossed.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getCrossLocations(matchData.getUserId());

                    }
                });

                name.setText(matchData.getName());
                interests.setText(matchData.getInterests());
                about.setText(matchData.getAbout());
                timesCrossed.setText(String.valueOf(matchData.getNumPathCrosses()));

                if (matchData.getPicture() != null) {
                    byte[] decoded_string = Base64.decode(matchData.getPicture(), Base64.DEFAULT);
                    if (decoded_string == null) {
                        Log.d("TAG", "ERROR!");
                    }
                    Bitmap decoded_byte = BitmapFactory.decodeByteArray(decoded_string, 0, decoded_string.length);
                    picture.setImageBitmap(decoded_byte);
                } else {
                    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.default_profile);
                    picture.setImageBitmap(icon);
                }

                mBuilder.setView(mView);
                AlertDialog dialog = mBuilder.create();
                dialog.show();

            }
        });
    }
}
