package me.vvander.wander;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MyLocation extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = MyLocation.class.getSimpleName();
    private int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 0;
    private GoogleMap mMap;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private RequestQueue requestQueue;
    private ArrayList<LatLng> listPersonal;
    private ArrayList<LatLng> listAll;

    private TileOverlay overlayPersonal;
    private TileOverlay overlayAll;
    private boolean overlayAllOn;
    private boolean overlayPersonalOn;

    private ArrayList<LatLng> cross;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_location);
        requestQueue = Volley.newRequestQueue(this);
        listPersonal = new ArrayList<>();
        listAll = new ArrayList<>();
        overlayAllOn = false;
        overlayPersonalOn = false;
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        getHeatmapData();
        // buildGoogleApiClient();
        //createLocationRequest();
        // startLocationUpdates();

        if (getCallingActivity() != null) {
            Log.d(TAG, getCallingActivity().getClassName());
            Toast.makeText(getApplicationContext(), getCallingActivity().getClassName(), Toast.LENGTH_SHORT).show();
            if (getCallingActivity().getClassName().equalsIgnoreCase("me.vvander.wander.MyMatches")) {
                Intent in = getIntent();
                cross = (ArrayList<LatLng>)in.getSerializableExtra("cross");
            }
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (cross != null && cross.size() > 0) {
            Toast.makeText(getApplicationContext(), "Here", Toast.LENGTH_SHORT).show();

            Log.d("Comes in here", "Comes into here onMapReady");
            for (int i = 0; i < cross.size(); i++) {
                Log.d("Comes in here", "Comes into here onMapReady 2");
                mMap.addMarker(new MarkerOptions()
                        .position(cross.get(i))
                        .title("Crossed here!"));
            }
        }
        while (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(17).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(getApplicationContext(), "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        //Toast.makeText(getApplicationContext(), "Current location:\n" + location, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            //LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    private void startLocationUpdates() {
        while (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
            //LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

    }

    private void getHeatmapData() {
        Map<String, String> params = new HashMap<>();
        String url = Data.getInstance().getUrl() + "/getLocationForHeatmap";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
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
                        Toast.makeText(getApplicationContext(), "Heatmap data point retrieval failed", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, error.toString());
                    }
                }
        );

        url = Data.getInstance().getUrl() + "/getAllLocationsForHeatmap";
        JsonObjectRequest postRequest2 = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
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
                        Toast.makeText(getApplicationContext(), "Heatmap data point retrieval failed", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
        requestQueue.add(postRequest2);
    }

    public void displayHeatmap(View view) {
        TextView heatmap = findViewById(R.id.heatmapButton);
        /*
        if (overlayPersonal == null && heatmap.getText().toString().equals("Display Heatmap")) {
            Toast.makeText(getApplicationContext(), "Displaying Heatmap", Toast.LENGTH_SHORT).show();
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder().data(listPersonal).build();
            overlayPersonal = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
            heatmap.setText("Remove Heatmap");
        } else if (heatmap.getText().toString().equals("Display Heatmap")) {
            heatmap.setText("Remove Heatmap");
            overlayPersonal.setVisible(true);
        } else if (heatmap.getText().toString().equals("Remove Heatmap")) {
            heatmap.setText("Display Heatmap");
            overlayPersonal.setVisible(false);
        }
        */
        if (overlayPersonal == null && overlayPersonalOn == false) {
            if (listPersonal.size() > 0) {
                HeatmapTileProvider provider = new HeatmapTileProvider.Builder().data(listPersonal).build();
                overlayPersonal = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
                overlayPersonalOn = true;
                Toast.makeText(getApplicationContext(), "Displaying Heatmap", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(getApplicationContext(), "No Location History Yet", Toast.LENGTH_SHORT).show();
            }
        } else if (overlayPersonalOn == false) {
            overlayPersonal.setVisible(true);
            overlayPersonalOn = true;
            Toast.makeText(getApplicationContext(), "Displaying Heatmap", Toast.LENGTH_SHORT).show();
        } else if (overlayPersonalOn == true) {
            overlayPersonal.setVisible(false);
            overlayPersonalOn = false;
            Toast.makeText(getApplicationContext(), "Removing Heatmap", Toast.LENGTH_SHORT).show();
        }
    }

    public void displayHeatmapAll(View view) {
        TextView heatmap = findViewById(R.id.allHeatmapButton);

        int[] colors = {
                Color.parseColor("#00A99D"),
                Color.parseColor("#93278F")
        };

        float[] startPoints = {
                0.2f, 1f
        };

        Gradient gradient = new Gradient(colors, startPoints);
        /*
        if (overlayAll == null && heatmap.getText().toString().equals("Display Popular Locations")) {
            Toast.makeText(getApplicationContext(), "Displaying Popular Locations", Toast.LENGTH_SHORT).show();
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder().data(listAll).build();
            overlayAll = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
            heatmap.setText("Remove Popular Locations");
        } else if (heatmap.getText().toString().equals("Display Popular Locations")) {
            heatmap.setText("Remove Popular Locations");
            overlayAll.setVisible(true);
        } else if (heatmap.getText().toString().equals("Remove Popular Locations")) {
            heatmap.setText("Display Popular Locations");
            overlayAll.setVisible(false);
        }
        */
        if (overlayAll == null && overlayAllOn == false) {
            if (listAll.size() > 0) {
                HeatmapTileProvider provider = new HeatmapTileProvider.Builder().gradient(gradient).data(listPersonal).build();
                overlayAll = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
                overlayAllOn = true;
                Toast.makeText(getApplicationContext(), "Displaying Heatmap", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "No Location History Yet", Toast.LENGTH_SHORT).show();
            }
        } else if (overlayAllOn == false) {
            overlayAll.setVisible(true);
            overlayAllOn = true;
            Toast.makeText(getApplicationContext(), "Displaying Heatmap", Toast.LENGTH_SHORT).show();
        } else if (overlayAllOn == true) {
            overlayAll.setVisible(false);
            overlayAllOn = false;
            Toast.makeText(getApplicationContext(), "Removing Heatmap", Toast.LENGTH_SHORT).show();
        }
    }

    public void requestPermission() {
        while (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
        }
    }
}