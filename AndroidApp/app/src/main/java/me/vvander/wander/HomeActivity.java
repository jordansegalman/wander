package me.vvander.wander;

import android.Manifest;
import android.app.AlarmManager;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
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

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final int ACTIVITY_RECOGNITION_DETECTION_INTERVAL = 15000;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2222;
    private static final String SP_LOCATION = "locationSwitch";
    private static final String SP_SCHEDULE = "locationSchedule";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeManualLocationSwitch();
        initializeScheduleLocationSwitch();
        setupLocationScheduleAlarm();
        setupActivityRecognition();

        toolbar = findViewById(R.id.toolbar);
        requestQueue = Volley.newRequestQueue(this);
        listPersonal = new ArrayList<>();
        listAll = new ArrayList<>();
        overlayPersonalOn = false;
        overlayAllOn = false;

        setSupportActionBar(toolbar);
        getHeatmapData();

        String json = getIntent().getStringExtra("Cross List");
        if (json != null) {
            Gson gson = new Gson();
            crossList = gson.fromJson(json, new TypeToken<ArrayList<LatLng>>() {}.getType());
        } else {
            setupDrawer();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeManualLocationSwitch() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_LOCATION, Context.MODE_PRIVATE);
        Data.getInstance().setManualLocationSwitch(sharedPreferences.getBoolean("manualLocationSwitch", true));
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
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
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
                        .title("Crossed paths here!"));
            }
            this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(crossList.get(crossList.size() - 1), 15));
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
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
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
            }
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    private void getHeatmapData() {
        String url = Data.getInstance().getUrl() + "/getLocationForHeatmap";
        JsonObjectRequest firstPostRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("Location");
                            int length = array.length();
                            for (int i = 0; i < length; i++) {
                                JSONObject object = array.getJSONObject(i);
                                double lat = Double.parseDouble(String.valueOf(object.get("latitude")));
                                double lon = Double.parseDouble(String.valueOf(object.get("longitude")));
                                listPersonal.add(new LatLng(lat, lon));
                                Log.d(TAG, "Latitude: " + lat + ", Longitude: " + lon);
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
                            int length = array.length();
                            for (int i = 0; i < length; i++) {
                                JSONObject object = array.getJSONObject(i);
                                double lat = Double.parseDouble(String.valueOf(object.get("latitude")));
                                double lon = Double.parseDouble(String.valueOf(object.get("longitude")));
                                listAll.add(new LatLng(lat, lon));
                                Log.d(TAG, "Latitude: " + lat + ", Longitude: " + lon);
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

    public void displayHeatmapPersonal() {
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

    public void displayHeatmapAll() {
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