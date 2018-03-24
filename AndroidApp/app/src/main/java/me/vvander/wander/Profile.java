package me.vvander.wander;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class Profile extends AppCompatActivity {
    private static final String TAG = "Profile";
    private RequestQueue requestQueue;
    private TextView nameText;
    private TextView interestText;
    private TextView aboutText;
    private TextView locationText;
    private TextView emailText;
    private ImageView profilePicture;

    private TextView nameText_input;
    private TextView interestText_input;
    private TextView aboutText_input;
    private TextView locationText_input;
    private TextView emailText_input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        requestQueue = Volley.newRequestQueue(this);

        String name = "Name";
        String interests = "Interests";
        String about = "About";
        String location = "Location";
        String email = "Email";

        profilePicture = (ImageView)findViewById(R.id.picture);
        nameText = (TextView)findViewById(R.id.name);
        interestText = (TextView)findViewById(R.id.interests);
        aboutText = (TextView)findViewById(R.id.about);
        locationText = (TextView)findViewById(R.id.location);
        emailText = (TextView)findViewById(R.id.email);

        nameText_input = (TextView)findViewById(R.id.name);
        interestText_input = (TextView)findViewById(R.id.interests_text);
        aboutText_input = (TextView)findViewById(R.id.about_text);
        locationText_input = (TextView)findViewById(R.id.location_text);
        emailText_input = (TextView)findViewById(R.id.email_text);


        if (getCallingActivity() != null) {
            Log.d("This is the callingactivity", getCallingActivity().getClassName());
            Log.d(TAG, getCallingActivity().getClassName());
            if (getCallingActivity().getClassName().equalsIgnoreCase("me.vvander.wander.ProfileEdit")) {
                Intent in = getIntent();
                nameText_input.setText(in.getExtras().getString("name"));
                interestText_input.setText(in.getExtras().getString("interests"));
                aboutText_input.setText(in.getExtras().getString("about"));
                emailText_input.setText(in.getExtras().getString("email"));
                locationText_input.setText(in.getExtras().getString("location"));
                if (in.getExtras().getString("picture") != null) {
                    byte[] decoded_string = Base64.decode(in.getExtras().getString("picture"), Base64.DEFAULT);
                    if (decoded_string == null) {
                        Log.d("ERROR MESSAGE", "ERROR!");
                    }
                    Bitmap decoded_byte = BitmapFactory.decodeByteArray(decoded_string, 0, decoded_string.length);
                    profilePicture.setImageBitmap(decoded_byte);
                } else {
                    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.default_profile);
                    profilePicture.setImageBitmap(icon);
                }
            }
        } else {
            sendPOSTRequest();
        }

        nameText_input.setVisibility(View.VISIBLE);
        interestText_input.setVisibility(View.VISIBLE);
        aboutText_input.setVisibility(View.VISIBLE);
        locationText_input.setVisibility(View.VISIBLE);
        emailText_input.setVisibility(View.VISIBLE);
        profilePicture.setVisibility(View.VISIBLE);

        //profilePicture.setBackground();
        //nameText.setText(name);
        //interestText.setText(interests);
        //aboutText.setText(about);
        //locationText.setText(location);
        //emailText.setText(email);
    }

    public void linkedInProfile(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("https://vvander.me/linkedInProfile"));
        startActivity(intent);
    }

    public void update(View view) {
        sendPOSTRequest();
    }

    private void sendPOSTRequest() {
        String url = Data.getInstance().getUrl() + "/getProfile";

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {

                                String first = response.getString("firstName");
                                //String last = response.getString("lastName");
                                //String e = response.getString("email");
                                String location = response.getString("loc");
                                String about = response.getString("about");
                                String interests = response.getString("interests");
                                String picture = response.getString("picture");

                                Log.d("Raw string", picture);
                                //String name = first + " " + last;
                                String name = first;

                                nameText_input.setText(name);
                                locationText_input.setText(location);
                                //emailText_input.setText(e);
                                aboutText_input.setText(about);
                                interestText_input.setText(interests);
                                if (picture != null && !picture.equalsIgnoreCase("null")) {
                                    byte[] decoded_string = Base64.decode(picture.getBytes(), Base64.DEFAULT);
                                    if (decoded_string == null)
                                    {
                                        Log.d("ERROR MESSAGE", "ERROR!");
                                    }
                                    Log.d("Decoded string", decoded_string.toString());
                                    Bitmap decoded_byte = BitmapFactory.decodeByteArray(decoded_string, 0, decoded_string.length);
                                    profilePicture.setImageBitmap(decoded_byte);
                                } else {
                                    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.default_profile);
                                    profilePicture.setImageBitmap(icon);
                                }

                                //Toast.makeText(getApplicationContext(), "Profile Updated!", Toast.LENGTH_SHORT).show();
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

                        Log.d("Error: ", error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    public void edit(View view){
        /*
        EditText etInterests = (EditText) findViewById(R.id.interests_text);
        EditText etAbout = (EditText) findViewById(R.id.about_text);
        EditText etLocation = (EditText) findViewById(R.id.location_text);
        EditText etEmail = (EditText) findViewById(R.id.location_text);
        EditText etName = (EditText) findViewById(R.id.name);
        */
        Intent i = new Intent(Profile.this, ProfileEdit.class);
        i.putExtra("location", locationText_input.getText().toString());
        i.putExtra("about", aboutText_input.getText().toString());
        i.putExtra("interests", interestText_input.getText().toString());
        i.putExtra("email", emailText_input.getText().toString());
        i.putExtra("name", nameText_input.getText().toString());

        BitmapDrawable drawable = (BitmapDrawable) profilePicture.getDrawable();
        if (drawable != null) {
            Bitmap bitmap = drawable.getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] b = baos.toByteArray();
            String encoded_picture = Base64.encodeToString(b, Base64.DEFAULT);

            i.putExtra("picture", encoded_picture);
        }


        startActivity(i);
        this.finish();

        //startActivity(new Intent(Profile.this, ProfileEdit.class));
    }
}