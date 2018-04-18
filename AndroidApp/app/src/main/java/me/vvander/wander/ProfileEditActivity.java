package me.vvander.wander;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends AppCompatActivity {
    private static final String TAG = ProfileEditActivity.class.getSimpleName();
    private static final int REQUEST_IMAGE_GET = 1;
    private RequestQueue requestQueue;
    private ImageView pictureImageView;
    private EditText nameEditText;
    private EditText aboutEditText;
    private String originalPicture;
    private String originalName;
    private String originalAbout;
    private String originalInterests;
    private String firstInterest;
    private String secondInterest;
    private String thirdInterest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        requestQueue = Volley.newRequestQueue(this);

        pictureImageView = findViewById(R.id.picture);
        nameEditText = findViewById(R.id.name);
        aboutEditText = findViewById(R.id.about);
        Spinner firstInterestSpinner = findViewById(R.id.first_interest_spinner);
        firstInterestSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Object item = adapterView.getItemAtPosition(position);
                if (item != null && !item.toString().equals("Select an interest")) {
                    firstInterest = item.toString();
                } else if (item != null && item.toString().equals("Select an interest")) {
                    firstInterest = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        Spinner secondInterestSpinner = findViewById(R.id.second_interest_spinner);
        secondInterestSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Object item = adapterView.getItemAtPosition(position);
                if (item != null && !item.toString().equals("Select an interest")) {
                    secondInterest = item.toString();
                } else if (item != null && item.toString().equals("Select an interest")) {
                    secondInterest = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        Spinner thirdInterestSpinner = findViewById(R.id.third_interest_spinner);
        thirdInterestSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Object item = adapterView.getItemAtPosition(position);
                if (item != null && !item.toString().equals("Select an interest")) {
                    thirdInterest = item.toString();
                } else if (item != null && item.toString().equals("Select an interest")) {
                    thirdInterest = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        originalPicture = getIntent().getStringExtra("picture");
        originalName = getIntent().getStringExtra("name");
        originalAbout = getIntent().getStringExtra("about");
        originalInterests = getIntent().getStringExtra("interests");

        pictureImageView.setImageBitmap(Utilities.decodeImage(originalPicture));
        nameEditText.setText(originalName);
        aboutEditText.setText(originalAbout);
        if (originalInterests.equals("No Interests")) {
            firstInterest = "";
            secondInterest = "";
            thirdInterest = "";
        } else {
            String[] splitInterests = originalInterests.split(", ");
            if (splitInterests.length == 1) {
                firstInterest = splitInterests[0];
                secondInterest = "";
                thirdInterest = "";
                firstInterestSpinner.setSelection(((ArrayAdapter) firstInterestSpinner.getAdapter()).getPosition(firstInterest));
            } else if (splitInterests.length == 2) {
                firstInterest = splitInterests[0];
                secondInterest = splitInterests[1];
                thirdInterest = "";
                firstInterestSpinner.setSelection(((ArrayAdapter) firstInterestSpinner.getAdapter()).getPosition(firstInterest));
                secondInterestSpinner.setSelection(((ArrayAdapter) secondInterestSpinner.getAdapter()).getPosition(secondInterest));
            } else if (splitInterests.length == 3) {
                firstInterest = splitInterests[0];
                secondInterest = splitInterests[1];
                thirdInterest = splitInterests[2];
                firstInterestSpinner.setSelection(((ArrayAdapter) firstInterestSpinner.getAdapter()).getPosition(firstInterest));
                secondInterestSpinner.setSelection(((ArrayAdapter) secondInterestSpinner.getAdapter()).getPosition(secondInterest));
                thirdInterestSpinner.setSelection(((ArrayAdapter) thirdInterestSpinner.getAdapter()).getPosition(thirdInterest));
            }
        }
    }

    public void selectImage(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_GET);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                pictureImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void done(View view) {
        String encodedImage = Utilities.encodeImage(((BitmapDrawable) pictureImageView.getDrawable()).getBitmap());
        String name = nameEditText.getText().toString();
        String about = aboutEditText.getText().toString();
        String interests;
        if (firstInterest.isEmpty() && secondInterest.isEmpty() && thirdInterest.isEmpty()) {
            interests = "No Interests";
        } else {
            interests = "";
            if (!firstInterest.isEmpty()) {
                interests += firstInterest + ", ";
            }
            if (!secondInterest.isEmpty()) {
                interests += secondInterest + ", ";
            }
            if (!thirdInterest.isEmpty()) {
                interests += thirdInterest + ", ";
            }
            interests = interests.substring(0, interests.length() - 2);
        }

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getApplicationContext(), "Name required.", Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(about)) {
            Toast.makeText(getApplicationContext(), "About required.", Toast.LENGTH_SHORT).show();
            return;
        } else if (encodedImage.equals(originalPicture) && name.equals(originalName) && about.equals(originalAbout) && interests.equals(originalInterests)) {
            startActivity(new Intent(ProfileEditActivity.this, ProfileActivity.class));
            finish();
            return;
        }

        String url = Data.getInstance().getUrl() + "/updateProfile";
        Map<String, String> params = new HashMap<>();
        params.put("name", nameEditText.getText().toString());
        params.put("about", aboutEditText.getText().toString());
        params.put("interests", interests);
        params.put("picture", encodedImage);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                                notifyInterestsChange();
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null) {
                            Toast.makeText(getApplicationContext(), new String(networkResponse.data), Toast.LENGTH_LONG).show();
                        }
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

    private void notifyInterestsChange() {
        String url = Data.getInstance().getUrl() + "/notifyInterestsChange";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                startActivity(new Intent(ProfileEditActivity.this, ProfileActivity.class));
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

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ProfileEditActivity.this, ProfileActivity.class));
        finish();
    }
}