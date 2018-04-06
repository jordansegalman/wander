package me.vvander.wander;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Profile extends AppCompatActivity {
    private static final String TAG = Profile.class.getSimpleName();
    private RequestQueue requestQueue;
    private ImageView pictureImageView;
    private TextView nameTextView;
    private TextView aboutTextView;
    private TextView interestsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        requestQueue = Volley.newRequestQueue(this);

        Button importFacebookButton = findViewById(R.id.importFacebookButton);

        if (Data.getInstance().getLoggedIn() || Data.getInstance().getLoggedInGoogle()) {
            importFacebookButton.setVisibility(View.GONE);
        } else if (Data.getInstance().getLoggedInFacebook()) {
            importFacebookButton.setVisibility(View.VISIBLE);
        }

        pictureImageView = findViewById(R.id.picture);
        nameTextView = findViewById(R.id.name);
        aboutTextView = findViewById(R.id.about);
        interestsTextView = findViewById(R.id.interests);

        if (getCallingActivity() != null && getCallingActivity().getClassName().equalsIgnoreCase("me.vvander.wander.ProfileEdit")) {
            nameTextView.setText(getIntent().getStringExtra("name"));
            aboutTextView.setText(getIntent().getStringExtra("about"));
            interestsTextView.setText(getIntent().getStringExtra("interests"));
            pictureImageView.setImageBitmap(Utilities.decodeImage(getIntent().getStringExtra("picture")));
        } else {
            getProfile();
        }

        pictureImageView.setVisibility(View.VISIBLE);
        nameTextView.setVisibility(View.VISIBLE);
        interestsTextView.setVisibility(View.VISIBLE);
        aboutTextView.setVisibility(View.VISIBLE);
    }

    public void edit(View view) {
        Intent intent = new Intent(Profile.this, ProfileEdit.class);
        intent.putExtra("name", nameTextView.getText().toString());
        intent.putExtra("about", aboutTextView.getText().toString());
        intent.putExtra("interests", interestsTextView.getText().toString());
        intent.putExtra("picture", Utilities.encodeImage(((BitmapDrawable) pictureImageView.getDrawable()).getBitmap()));
        startActivity(intent);
    }

    public void update(View view) {
        getProfile();
    }

    public void importFacebookProfile(View view) {
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            String name = object.getString("first_name");
                            sendFacebookProfileToServer(name);
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Failed to import Facebook profile!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void sendFacebookProfileToServer(final String name) {
        String url = Data.getInstance().getUrl() + "/getProfile";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                String about = response.getString("about");
                                String interests = response.getString("interests");
                                String picture = response.getString("picture");

                                Map<String, String> params = new HashMap<>();
                                params.put("name", name);
                                params.put("about", about);
                                params.put("interests", interests);
                                params.put("picture", picture);

                                String url = Data.getInstance().getUrl() + "/updateProfile";

                                JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                                        new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                try {
                                                    String res = response.getString("response");
                                                    if (res.equalsIgnoreCase("pass")) {
                                                        getProfile();
                                                        Toast.makeText(getApplicationContext(), "Facebook profile imported!", Toast.LENGTH_SHORT).show();
                                                    }
                                                } catch (JSONException j) {
                                                    j.printStackTrace();
                                                }
                                            }
                                        },
                                        new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Toast.makeText(getApplicationContext(), "Failed to import Facebook profile!", Toast.LENGTH_SHORT).show();
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
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Failed to import Facebook profile!", Toast.LENGTH_SHORT).show();
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

    public void importLinkedInProfile(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("https://vvander.me/linkedInProfile"));
        startActivity(intent);
    }

    private void getProfile() {
        String url = Data.getInstance().getUrl() + "/getProfile";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                String name = response.getString("name");
                                String about = response.getString("about");
                                String interests = response.getString("interests");
                                String picture = response.getString("picture");

                                nameTextView.setText(name);
                                aboutTextView.setText(about);
                                interestsTextView.setText(interests);
                                if (picture != null && !picture.equalsIgnoreCase("null")) {
                                    pictureImageView.setImageBitmap(Utilities.decodeImage(picture));
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
                        Toast.makeText(getApplicationContext(), "No profile found!", Toast.LENGTH_SHORT).show();
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