package me.vvander.wander;

import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

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

public class BlockedActivity extends AppCompatActivity {
    private static final String TAG = AppCompatActivity.class.getSimpleName();
    private RequestQueue requestQueue;
    private ArrayList<Blocked> blockedList;
    private int remainingBlockedToAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked);
        requestQueue = Volley.newRequestQueue(this);
        blockedList = new ArrayList<>();
        remainingBlockedToAdd = 0;
        requestAllBlocked();
    }

    private void requestAllBlocked() {
        String url = Data.getInstance().getUrl() + "/getAllBlocked";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("UIDs");
                            int length = array.length();
                            remainingBlockedToAdd = length;
                            if (length == 0) {
                                Log.d(TAG, "HELLO!!!");
                                setupListView();
                            } else {
                                for (int i = 0; i < length; i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String uid = object.getString("uid");
                                    requestSingleBlocked(uid);
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

    private void requestSingleBlocked(String uid) {
        String url = Data.getInstance().getUrl() + "/getBlocked";
        Map<String, String> params = new HashMap<>();
        params.put("uid", uid);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("Blocked");
                            JSONObject object = array.getJSONObject(0);
                            String uid = object.getString("uid");
                            String name = object.getString("name");
                            String picture = object.getString("picture");

                            if (picture == null || !picture.equalsIgnoreCase("null")) {
                                picture = Utilities.encodeImage(BitmapFactory.decodeResource(getResources(), R.drawable.default_profile));
                            }

                            Blocked blocked = new Blocked(uid, name, picture);
                            blockedList.add(blocked);
                            remainingBlockedToAdd--;
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
        Space noBlockedTopSpace = findViewById(R.id.noBlockedTopSpace);
        TextView noBlockedText = findViewById(R.id.noBlockedText);
        Space noBlockedBottomSpace = findViewById(R.id.noBlockedBottomSpace);
        ListView blockedListView = findViewById(R.id.blockedList);

        if (!blockedList.isEmpty() && remainingBlockedToAdd == 0) {
            noBlockedTopSpace.setVisibility(View.GONE);
            noBlockedText.setVisibility(View.GONE);
            noBlockedBottomSpace.setVisibility(View.GONE);
            blockedListView.setVisibility(View.VISIBLE);
            BlockedAdapter blockedAdapter = new BlockedAdapter(this, blockedList);
            blockedListView.setAdapter(blockedAdapter);
            blockedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final Blocked blocked = (Blocked) parent.getAdapter().getItem(position);
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("Are you sure you want to unblock " + blocked.getName() + "?")
                            .setPositiveButton("Unblock", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    unblock(blocked);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        } else if (blockedList.isEmpty()) {
            noBlockedTopSpace.setVisibility(View.VISIBLE);
            noBlockedText.setVisibility(View.VISIBLE);
            noBlockedBottomSpace.setVisibility(View.VISIBLE);
            blockedListView.setVisibility(View.GONE);
            blockedListView.setEnabled(false);
        }
    }

    private void unblock(final Blocked blocked) {
        String url = Data.getInstance().getUrl() + "/unblockUser";
        java.util.Map<String, String> params = new HashMap<>();
        params.put("uid", blocked.getUserID());
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                blockedList.clear();
                                remainingBlockedToAdd = 0;
                                requestAllBlocked();
                                Toast.makeText(getApplicationContext(), "User unblocked.", Toast.LENGTH_SHORT).show();
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