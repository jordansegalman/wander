package me.vvander.wander;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import me.vvander.wander.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileEdit extends AppCompatActivity {
    private EditText etInterests;
    private EditText etAbout;
    private EditText etLocation;
    private EditText etEmail;
    private TextView etName;
    private CircleImageView civPicture;
    private Spinner spinner;
    private RequestQueue requestQueue;

    private final int FINISHED = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        requestQueue = Volley.newRequestQueue(this);

        etInterests = (EditText) findViewById(R.id.interests_text);
        etAbout = (EditText) findViewById(R.id.about_text);
        etLocation = (EditText) findViewById(R.id.location_text);
        etEmail = (EditText) findViewById(R.id.email_text);
        etName = findViewById(R.id.name);
        spinner = (Spinner)findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Object item = adapterView.getItemAtPosition(position);
                if (item != null && !item.toString().equals("Select an interest")) {
                    etInterests.setText(item.toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        civPicture = findViewById(R.id.picture);

        Intent in = getIntent();
        String loc = in.getExtras().getString("location");
        String about = in.getExtras().getString("about");
        String interests = in.getExtras().getString("interests");
        String name = in.getExtras().getString("name");
        String email = in.getExtras().getString("email");
        String picture = in.getExtras().getString("picture");

        etInterests.setText(interests);
        etAbout.setText(about);
        etLocation.setText(loc);
        etName.setText(name);
        etEmail.setText(email);

        if (picture != null) {
            byte[] decoded_string = Base64.decode(picture, Base64.DEFAULT);
            if (decoded_string == null) {
                Log.d("ERROR MESSAGE", "ERROR!");
            }
            Bitmap decoded_byte = BitmapFactory.decodeByteArray(decoded_string, 0, decoded_string.length);
            civPicture.setImageBitmap(decoded_byte);
        }
    }

    public void selectImage(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent.createChooser(intent, "Select Profile Picture"), FINISHED);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (data != null && requestCode == FINISHED) {
            try {
                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                CircleImageView circleImageView = findViewById(R.id.picture);
                circleImageView.setImageBitmap(BitmapFactory.decodeStream(inputStream));
            } catch (IOException e) {
                Log.d("tag", e.toString());
            }

        }
    }

    public void done(View view){
        //TODO: Save profile data
        etInterests = (EditText) findViewById(R.id.interests_text);
        etAbout = (EditText) findViewById(R.id.about_text);
        etLocation = (EditText) findViewById(R.id.location_text);
        etEmail = (EditText) findViewById(R.id.email_text);
        etName = (TextView) findViewById(R.id.name);

        BitmapDrawable drawable = (BitmapDrawable) civPicture.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] b = baos.toByteArray();
        String encoded_picture = Base64.encodeToString(b, Base64.DEFAULT);
        Log.d("Encoded Picture", encoded_picture);


        Intent i = new Intent(ProfileEdit.this, Profile.class);
        i.putExtra("location", etLocation.getText().toString());
        i.putExtra("about", etAbout.getText().toString());
        i.putExtra("interests", etInterests.getText().toString());
        i.putExtra("email", etEmail.getText().toString());
        i.putExtra("name", etName.getText().toString());
        i.putExtra("picture", encoded_picture);

        String url = Data.getInstance().getUrl() + "/updateProfile";
        Map<String, String> params = new HashMap<String,String>();
        params.put("name", etName.getText().toString());
        //params.put("email", etEmail.getText().toString());
        params.put("loc", etLocation.getText().toString());
        params.put("about", etAbout.getText().toString());
        params.put("interests", etInterests.getText().toString());
        params.put("picture", encoded_picture);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "User data stored", Toast.LENGTH_SHORT).show();

                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error: ", error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);


        ActivityCompat.startActivityForResult(this, i, 0, null);
        this.finish();
        //startActivity(i);
    }
}