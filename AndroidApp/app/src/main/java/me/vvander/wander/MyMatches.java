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
    ArrayList<String> matchList;
    Map<String, MatchData> matchMap = new HashMap<String, MatchData>();
    ListView matchListView;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_matches);
        requestQueue = Volley.newRequestQueue(this);

        populateList();
        setupListView();

    }

    private void populateList(){
        //TODO: get matches information and put it in matchList
    }

    private void requestAllMatches() {
        String url = Data.getInstance().getUrl() + "/getAllMatches";
        Map<String, String> params = new HashMap<>();

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
                                //String picture = String.valueOf(object.get("picture"));
                                //String about = String.valueOf(object.get("about"));
                                //String interests = String.valueOf(object.get("interests"));
                                //String name = String.valueOf(object.get("firstname"));
                                requestSingleMatch(uid);
                                //MatchData match = new MatchData();
                                //match.setAbout(String.valueOf(object.get("about")));
                                //match.setUserId(String.valueOf(object.get("uid")));
                                //match.setInterests(String.valueOf(object.get("interests")));
                                //match.setPicture(String.valueOf(object.get("picture")));
                                //match.setName(String.valueOf(object.get("firstname")));
                                //matchMap.put(uid, match);

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
                            //String picture = String.valueOf(object.get("picture"));
                            //String about = String.valueOf(object.get("about"));
                            //String interests = String.valueOf(object.get("interests"));
                            //String name = String.valueOf(object.get("firstname"));

                            MatchData match = new MatchData();
                            match.setAbout(String.valueOf(object.get("about")));
                            match.setUserId(String.valueOf(object.get("uid")));
                            match.setInterests(String.valueOf(object.get("interests")));
                            match.setPicture(String.valueOf(object.get("picture")));
                            match.setName(String.valueOf(object.get("firstname")));
                            matchMap.put(uid, match);


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

        String[] listItems;

        if(matchMap == null || matchMap.isEmpty()){
            //listItems = new String[1];
            //listItems[0] = "No New Matches";
            matchList.add("No New Matches");
        }
        else {
            listItems = new String[matchMap.size()];

            for (int i = 0; i < matchMap.size(); i++) {
                //listItems[i] = matchMap.get(i).getName();
                Iterator it = matchMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    MatchData matchData = (MatchData) pair.getValue();
                    //listItems[i] = matchData.getName();
                    matchList.add(matchData.getName());
                    it.remove();
                }
            }
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_1, matchList);
        matchListView = (ListView) findViewById(R.id.matchesList);
        matchListView.setAdapter(adapter);

        matchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*TODO: when a user clicks on a match in the list this is triggered. The value of position is equal
                to the index of the corresponding matchData in matchList*/

            }
        });
    }
}
