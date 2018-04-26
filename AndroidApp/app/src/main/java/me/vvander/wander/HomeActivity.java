package me.vvander.wander;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMarkerClickListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final int ACTIVITY_RECOGNITION_DETECTION_INTERVAL = 15000;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2222;
    private static final int PLACE_PICKER_REQUEST = 3333;
    private static final String SP_LOCATION = "locationSwitch";
    private static final String SP_SCHEDULE = "locationSchedule";
    private static final String SP_NOTIFICATIONS = "notificationSwitch";
    private Toolbar toolbar;
    private RequestQueue requestQueue;
    private GoogleMap googleMap;
    private ArrayList<LatLng> listPersonal;
    private ArrayList<LatLng> listAll;
    private TileOverlay overlayPersonal;
    private TileOverlay overlayAll;
    private boolean overlayPersonalOn;
    private boolean overlayAllOn;
    private ArrayList<LatLng> crossList;
    private Map<Marker, LocationTag> locationTags;
    private Map<Marker, LocationTag> matchLocationTags;
    private ArrayList<Marker> locationSuggestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getThemeNoActionBar(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        requestQueue = Volley.newRequestQueue(this);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        String json = getIntent().getStringExtra("Cross List");
        if (json != null) {
            Gson gson = new Gson();
            crossList = gson.fromJson(json, new TypeToken<ArrayList<LatLng>>() {
            }.getType());
        } else {
            setupDrawer();
            initializeManualLocationSwitch();
            initializeScheduleLocationSwitch();
            initializeNotificationSwitch();
            setupLocationScheduleAlarm();
            setupActivityRecognition();
        }

        listPersonal = new ArrayList<>();
        listAll = new ArrayList<>();
        overlayPersonalOn = false;
        overlayAllOn = false;
        locationTags = new HashMap<>();
        matchLocationTags = new HashMap<>();
        locationSuggestions = new ArrayList<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.googleMap != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && Data.getInstance().getManualLocationSwitch() && Data.getInstance().getScheduleLocationSwitch()) {
                this.googleMap.setMyLocationEnabled(true);
                this.googleMap.setOnMyLocationButtonClickListener(this);
            } else {
                this.googleMap.setMyLocationEnabled(false);
            }
        }
        listPersonal.clear();
        listAll.clear();
        getHeatmapData();
        getTagData();
    }

    private void setupDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        switch (item.getItemId()) {
            case R.id.nav_map:
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            case R.id.nav_matches:
                startActivity(new Intent(HomeActivity.this, MatchesActivity.class));
                return true;
            case R.id.nav_statistics:
                startActivity(new Intent(HomeActivity.this, StatisticsActivity.class));
                return true;
            case R.id.nav_profile:
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                return true;
            case R.id.nav_settings:
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.location_history:
                displayHeatmapPersonal();
                return true;
            case R.id.popular_locations:
                displayHeatmapAll();
                return true;
            case R.id.tag_location:
                addTag();
                return true;
            case R.id.location_suggestions:
                getLocationSuggestions();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeManualLocationSwitch() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_LOCATION, Context.MODE_PRIVATE);
        Data.getInstance().setManualLocationSwitch(sharedPreferences.getBoolean("manualLocationSwitch", true));
    }

    private void initializeNotificationSwitch() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_NOTIFICATIONS, Context.MODE_PRIVATE);
        Data.getInstance().setNotificationSwitch(sharedPreferences.getBoolean("notificationSwitch", true));
    }

    private void initializeScheduleLocationSwitch() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_SCHEDULE, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString("schedule", null);
        if (json != null) {
            Gson gson = new Gson();
            Data.getInstance().setLocationSchedules((ArrayList<LocationSchedule>) gson.fromJson(json, new TypeToken<ArrayList<LocationSchedule>>() {
            }.getType()));
        }
    }

    private void setupLocationScheduleAlarm() {
        Intent intent = new Intent(getApplicationContext(), LocationScheduleAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 60000, pendingIntent);
        }
    }

    private void setupActivityRecognition() {
        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognitionClient activityRecognitionClient = new ActivityRecognitionClient(getApplicationContext());
        activityRecognitionClient.requestActivityUpdates(ACTIVITY_RECOGNITION_DETECTION_INTERVAL, pendingIntent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (crossList != null && crossList.size() > 0) {
            for (int i = 0; i < crossList.size(); i++) {
                this.googleMap.addMarker(new MarkerOptions()
                        .position(crossList.get(i))
                        .title("Crossed Paths")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            }
            this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(crossList.get(crossList.size() - 1), 15));
        }
        this.googleMap.setOnMarkerClickListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            if (Data.getInstance().getManualLocationSwitch() && Data.getInstance().getScheduleLocationSwitch()) {
                this.googleMap.setMyLocationEnabled(true);
                this.googleMap.setOnMyLocationButtonClickListener(this);
                if (crossList == null) {
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager != null) {
                        Criteria criteria = new Criteria();
                        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                        if (location != null) {
                            this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
                        }
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "Location tracking disabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (Data.getInstance().getManualLocationSwitch() && Data.getInstance().getScheduleLocationSwitch()) {
                    googleMap.setMyLocationEnabled(true);
                    googleMap.setOnMyLocationButtonClickListener(this);
                    if (crossList == null) {
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (locationManager != null) {
                            Criteria criteria = new Criteria();
                            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                            if (location != null) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
                            }
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Location tracking disabled.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    private void getHeatmapData() {
        String url = Data.getInstance().getUrl() + "/getLocationsForHeatmap";
        JsonObjectRequest firstPostRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("Location");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                double lat = object.getDouble("latitude");
                                double lon = object.getDouble("longitude");
                                listPersonal.add(new LatLng(lat, lon));
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Location history heatmap data retrieval failed.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, error.toString());
                    }
                }
        );
        firstPostRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(firstPostRequest);
        url = Data.getInstance().getUrl() + "/getAllLocationsForHeatmap";
        JsonObjectRequest secondPostRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("Location");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                double lat = object.getDouble("latitude");
                                double lon = object.getDouble("longitude");
                                listAll.add(new LatLng(lat, lon));
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Popular locations heatmap data retrieval failed.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, error.toString());
                    }
                }
        );
        secondPostRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(secondPostRequest);
    }

    private void displayHeatmapPersonal() {
        if (overlayPersonal == null && !overlayPersonalOn) {
            if (listPersonal.size() > 0) {
                HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder().data(listPersonal).build();
                overlayPersonal = googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
                overlayPersonalOn = true;
            } else {
                Toast.makeText(getApplicationContext(), "No location data.", Toast.LENGTH_SHORT).show();
            }
        } else if (overlayPersonal != null && !overlayPersonalOn) {
            overlayPersonal.setVisible(true);
            overlayPersonalOn = true;
        } else if (overlayPersonal != null) {
            overlayPersonal.setVisible(false);
            overlayPersonalOn = false;
        }
    }

    private void displayHeatmapAll() {
        int[] colors = {
                Color.rgb(0, 225, 215),
                Color.rgb(125, 0, 225)
        };
        float[] startPoints = {
                0.2f, 1f
        };
        Gradient gradient = new Gradient(colors, startPoints);
        if (overlayAll == null && !overlayAllOn) {
            if (listAll.size() > 0) {
                HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder().data(listAll).gradient(gradient).build();
                overlayAll = googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
                overlayAllOn = true;
            } else {
                Toast.makeText(getApplicationContext(), "No location data.", Toast.LENGTH_SHORT).show();
            }
        } else if (overlayAll != null && !overlayAllOn) {
            overlayAll.setVisible(true);
            overlayAllOn = true;
        } else if (overlayAll != null) {
            overlayAll.setVisible(false);
            overlayAllOn = false;
        }
    }

    private void addTag() {
        try {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            Marker marker = googleMap.addMarker(new MarkerOptions().position(place.getLatLng()).title("My Tag"));
            this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15));
            locationTags.put(marker, new LocationTag());
            storeTagData(marker);
        }
    }

    private void storeTagData(final Marker marker) {
        String url = Data.getInstance().getUrl() + "/storeTagData";
        Map<String, String> params = new HashMap<>();

        LocationTag locationTag = locationTags.get(marker);

        JSONObject tag = new JSONObject();
        try {
            tag.put("latitude", marker.getPosition().latitude);
            tag.put("longitude", marker.getPosition().longitude);
            tag.put("title", locationTag.getTitle());
            tag.put("description", locationTag.getDescription());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        params.put("tag", tag.toString());
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                Toast.makeText(getApplicationContext(), "Location tagged.", Toast.LENGTH_SHORT).show();
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
                                    case "Invalid tag title":
                                        Toast.makeText(getApplicationContext(), "Tag title has a maximum length of 255 characters.", Toast.LENGTH_LONG).show();
                                        break;
                                    case "Invalid tag description":
                                        Toast.makeText(getApplicationContext(), "Tag description has a maximum length of 512 characters.", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(getApplicationContext(), "Location tagging failed!", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "Location tagging failed!", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
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

    private void getTagData() {
        String url = Data.getInstance().getUrl() + "/getTagData";
        JsonObjectRequest firstPostRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        for (Marker marker : locationTags.keySet()) {
                            marker.remove();
                        }
                        for (Marker marker : matchLocationTags.keySet()) {
                            marker.remove();
                        }
                        locationTags.clear();
                        matchLocationTags.clear();
                        try {
                            JSONArray array = response.getJSONArray("Tags");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                double lat = object.getDouble("latitude");
                                double lon = object.getDouble("longitude");
                                String title = object.getString("title");
                                String description = object.getString("description");
                                Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("My Tag"));
                                locationTags.put(marker, new LocationTag(title, description));
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Location tag data retrieval failed.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, error.toString());
                    }
                }
        );
        firstPostRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(firstPostRequest);
        url = Data.getInstance().getUrl() + "/getAllMatches";
        JsonObjectRequest secondPostRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("UIDs");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                String uid = object.getString("uid");
                                getMatchTags(uid);
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
        secondPostRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(secondPostRequest);
    }

    private void getMatchTags(String uid) {
        String url = Data.getInstance().getUrl() + "/getMatchTagData";
        Map<String, String> params = new HashMap<>();
        params.put("uid", uid);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("Tags");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                double lat = object.getDouble("latitude");
                                double lon = object.getDouble("longitude");
                                String title = object.getString("title");
                                String description = object.getString("description");
                                Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Match Tag").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                matchLocationTags.put(marker, new LocationTag(title, description));
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

    private void deleteTagData(final Marker marker) {
        String url = Data.getInstance().getUrl() + "/deleteTagData";
        Map<String, String> params = new HashMap<>();

        LocationTag locationTag = locationTags.get(marker);

        JSONObject tag = new JSONObject();
        try {
            tag.put("latitude", marker.getPosition().latitude);
            tag.put("longitude", marker.getPosition().longitude);
            tag.put("title", locationTag.getTitle());
            tag.put("description", locationTag.getDescription());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        params.put("tag", tag.toString());
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equalsIgnoreCase("pass")) {
                                marker.remove();
                                locationTags.remove(marker);
                                Toast.makeText(getApplicationContext(), "Tag deleted.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Tag deletion failed!", Toast.LENGTH_SHORT).show();
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
    public boolean onMarkerClick(final Marker marker) {
        if (marker.getTitle() != null && !marker.getTitle().equals("My Tag") && !marker.getTitle().equals("Match Tag")) {
            return false;
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(this).setView(R.layout.activity_location_tag).create();
        alertDialog.show();

        final Button editButton = alertDialog.findViewById(R.id.edit_button);
        final Button doneButton = alertDialog.findViewById(R.id.done_button);
        Button deleteButton = alertDialog.findViewById(R.id.delete_button);
        final EditText tagTitleEditText = alertDialog.findViewById(R.id.tag_title);
        final EditText tagDescriptionEditText = alertDialog.findViewById(R.id.tag_description);

        tagTitleEditText.setEnabled(false);
        tagDescriptionEditText.setEnabled(false);

        if (marker.getTitle() != null && marker.getTitle().equals("Match Tag")) {
            editButton.setVisibility(View.GONE);
            doneButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            LocationTag locationTag = matchLocationTags.get(marker);
            tagTitleEditText.setText(locationTag.getTitle());
            tagDescriptionEditText.setText(locationTag.getDescription());
        } else if (marker.getTitle() != null && marker.getTitle().equals("My Tag")) {
            final LocationTag locationTag = locationTags.get(marker);
            tagTitleEditText.setText(locationTag.getTitle());
            tagDescriptionEditText.setText(locationTag.getDescription());
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tagTitleEditText.setEnabled(true);
                    tagDescriptionEditText.setEnabled(true);
                    editButton.setVisibility(View.GONE);
                    doneButton.setVisibility(View.VISIBLE);
                }
            });
            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tagTitleEditText.setEnabled(false);
                    tagDescriptionEditText.setEnabled(false);
                    editButton.setVisibility(View.VISIBLE);
                    doneButton.setVisibility(View.GONE);
                    String title = tagTitleEditText.getText().toString();
                    String description = tagDescriptionEditText.getText().toString();
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(getApplicationContext(), "Enter a tag title.", Toast.LENGTH_SHORT).show();
                    } else if (TextUtils.isEmpty(description)) {
                        Toast.makeText(getApplicationContext(), "Enter a tag description.", Toast.LENGTH_SHORT).show();
                    } else if (!title.equals(locationTag.getTitle()) || !description.equals(locationTag.getDescription())) {
                        locationTag.setTitle(tagTitleEditText.getText().toString());
                        locationTag.setDescription(tagDescriptionEditText.getText().toString());
                        locationTags.put(marker, locationTag);
                        storeTagData(marker);
                    }
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteTagData(marker);
                    alertDialog.dismiss();
                }
            });
        }
        return false;
    }

    private void getLocationSuggestions() {
        if (locationSuggestions.size() > 0) {
            for (Marker marker : locationSuggestions) {
                marker.remove();
            }
            locationSuggestions.clear();
            return;
        }
        String url = Data.getInstance().getUrl() + "/getLocationSuggestions";
        JsonArrayRequest postRequest = new JsonArrayRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject jsonObject = response.getJSONObject(i);
                                JSONArray centroid = jsonObject.getJSONArray("centroid");
                                double lat = centroid.getDouble(0);
                                double lon = centroid.getDouble(1);
                                Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Popular Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                locationSuggestions.add(marker);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Location suggestions data retrieval failed.", Toast.LENGTH_SHORT).show();
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
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}