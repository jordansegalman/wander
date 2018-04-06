package me.vvander.wander;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
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
    private EditText interestsEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        requestQueue = Volley.newRequestQueue(this);

        pictureImageView = findViewById(R.id.picture);
        nameEditText = findViewById(R.id.name);
        aboutEditText = findViewById(R.id.about);
        interestsEditText = findViewById(R.id.interests);
        Spinner spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Object item = adapterView.getItemAtPosition(position);
                if (item != null && !item.toString().equals("Select interest")) {
                    interestsEditText.setText(item.toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        if (getCallingActivity() != null && getCallingActivity().getClassName().equalsIgnoreCase("me.vvander.wander.ProfileActivity")) {
            nameEditText.setText(getIntent().getStringExtra("name"));
            aboutEditText.setText(getIntent().getStringExtra("about"));
            interestsEditText.setText(getIntent().getStringExtra("interests"));
            pictureImageView.setImageBitmap(Utilities.decodeImage(getIntent().getStringExtra("picture")));
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

        final Intent intent = new Intent(ProfileEditActivity.this, ProfileActivity.class);
        intent.putExtra("name", nameEditText.getText().toString());
        intent.putExtra("about", aboutEditText.getText().toString());
        intent.putExtra("interests", interestsEditText.getText().toString());
        intent.putExtra("picture", encodedImage);

        String url = Data.getInstance().getUrl() + "/updateProfile";
        Map<String, String> params = new HashMap<>();
        params.put("name", nameEditText.getText().toString());
        params.put("about", aboutEditText.getText().toString());
        params.put("interests", interestsEditText.getText().toString());
        params.put("picture", encodedImage);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
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
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ProfileEditActivity.this, ProfileActivity.class));
        finish();
    }
}