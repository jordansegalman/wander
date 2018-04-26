package me.vvander.wander;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FacebookLoginActivity extends AppCompatActivity {
    private static final String TAG = FacebookLoginActivity.class.getSimpleName();
    private CallbackManager callbackManager;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facebook_login);

        requestQueue = Volley.newRequestQueue(this);

        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        getFacebookInfo(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        startActivity(new Intent(FacebookLoginActivity.this, LoginActivity.class));
                    }

                    @Override
                    public void onError(FacebookException error) {
                        startActivity(new Intent(FacebookLoginActivity.this, LoginActivity.class));
                    }
                });
        LoginManager.getInstance().logInWithReadPermissions(FacebookLoginActivity.this, Arrays.asList("public_profile", "email"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoginManager.getInstance().logOut();
    }

    private void getFacebookInfo(AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            String id = object.getString("id");
                            String email = object.getString("email");
                            attemptLogin(id, email);
                        } catch (JSONException e) {
                            startActivity(new Intent(FacebookLoginActivity.this, LoginActivity.class));
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void attemptLogin(String id, String email) {
        Map<String, String> params = new HashMap<>();
        params.put("facebookID", id);
        params.put("email", email);

        String url = Data.getInstance().getUrl() + "/facebookLogin";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            String uid = response.getString("uid");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Facebook login successful!", Toast.LENGTH_SHORT).show();
                                Data.getInstance().loginFacebook();
                                Data.getInstance().setUid(uid);
                                sendFirebaseRegistrationTokenToServer();
                                startLocationCollectionService();
                                Intent intent = new Intent(FacebookLoginActivity.this, HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
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
                        if (error.networkResponse.data != null) {
                            try {
                                String res = new JSONObject(new String(error.networkResponse.data)).getString("response");
                                switch (res) {
                                    case "Invalid email":
                                        Toast.makeText(getApplicationContext(), "Invalid email!", Toast.LENGTH_LONG).show();
                                        break;
                                    case "Email taken":
                                        Toast.makeText(getApplicationContext(), "Email taken!", Toast.LENGTH_LONG).show();
                                        break;
                                    case "Account banned":
                                        Toast.makeText(getApplicationContext(), "Account banned!", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(getApplicationContext(), "Facebook login failed!", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "Facebook login failed!", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                        Intent intent = new Intent(FacebookLoginActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    private void sendFirebaseRegistrationTokenToServer() {
        if (Data.getInstance().getLoggedInFacebook() && Data.getInstance().getFirebaseRegistrationToken() != null) {
            Map<String, String> params = new HashMap<>();
            params.put("firebaseRegistrationToken", Data.getInstance().getFirebaseRegistrationToken());

            String url = Data.getInstance().getUrl() + "/addFirebaseRegistrationToken";

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String res = response.getString("response");
                                if (res.equalsIgnoreCase("pass")) {
                                    Log.d(TAG, "Firebase registration token successfully added to server.");
                                } else {
                                    Log.d(TAG, "Error adding Firebase registration token to server.");
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

    private void startLocationCollectionService() {
        if (Data.getInstance().getManualLocationSwitch() && Data.getInstance().getScheduleLocationSwitch() && Data.getInstance().getActivityRecognitionLocationSwitch()) {
            startService(new Intent(getApplicationContext(), LocationCollectionService.class));
        } else {
            stopService(new Intent(getApplicationContext(), LocationCollectionService.class));
        }
    }
}