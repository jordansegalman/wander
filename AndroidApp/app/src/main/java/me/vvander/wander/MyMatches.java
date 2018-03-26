package me.vvander.wander;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import me.vvander.wander.R;

public class MyMatches extends AppCompatActivity {
    ArrayList<MatchData> matchList;
    Map<String, MatchData> matchMap;
    ListView matchListView;
    private RequestQueue requestQueue;
    ProfileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_matches);
        requestQueue = Volley.newRequestQueue(this);
        matchMap = new HashMap<String, MatchData>();
        matchList = new ArrayList<MatchData>();

        requestAllMatches();


    }

    private void populateList(){
        //TODO: get matches information and put it in matchList
    }

    private void requestAllMatches() {
        String url = Data.getInstance().getUrl() + "/getAllMatches";
        Map<String, String> params = new HashMap<>();
        Toast.makeText(getApplicationContext(), "Comes here", Toast.LENGTH_SHORT).show();
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("UIDs");
                            int length = array.length();
                            for (int i = 0; i < length; i++) {
                                JSONObject object = array.getJSONObject(i);
                                String uid = String.valueOf(object.get("uid"));
                                Toast.makeText(getApplicationContext(), uid, Toast.LENGTH_SHORT).show();
                                requestSingleMatch(uid);
                            }
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
        Toast.makeText(getApplicationContext(), "Gets to single match request.", Toast.LENGTH_SHORT).show();

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Should only be one item in the array
                            Toast.makeText(getApplicationContext(), "Response from server.", Toast.LENGTH_SHORT).show();
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
                            String timesCrossed = String.valueOf(object.get("timesCrossed"));
                            Log.d("Times Crossed", timesCrossed);
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
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    private void setupListView(){
        if (adapter != null) {
            adapter.clear();
        }

        if(matchMap == null || matchMap.isEmpty()){
            MatchData matchData = new MatchData();
            matchData.setName("No New Matches");
            matchList.add(matchData);
        }
        else {
            for (int i = 0; i < matchMap.size(); i++) {
                //listItems[i] = matchMap.get(i).getName();
                Iterator it = matchMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
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

        matchListView = (ListView) findViewById(R.id.matchesList);
        matchListView.setAdapter(adapter);

        matchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*TODO: when a user clicks on a match in the list this is triggered. The value of position is equal
                to the index of the corresponding matchData in matchList*/
                //int position = (Integer) view.getTag();
                // Access the row position here to get the correct data item
                //MatchData matchData = getItem(position);
            }
        });
    }
}
