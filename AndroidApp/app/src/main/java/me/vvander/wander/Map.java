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

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class Map extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = Map.class.getSimpleName();
    private GoogleMap mMap;
    private RequestQueue requestQueue;
    private ArrayList<LatLng> listPersonal;
    private ArrayList<LatLng> listAll;
    private TileOverlay overlayPersonal;
    private TileOverlay overlayAll;
    private boolean overlayPersonalOn;
    private boolean overlayAllOn;
    private ArrayList<LatLng> cross;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_location);
        requestQueue = Volley.newRequestQueue(this);
        listPersonal = new ArrayList<>();
        listAll = new ArrayList<>();
        overlayPersonalOn = false;
        overlayAllOn = false;
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        getHeatmapData();
        if (getCallingActivity() != null) {
            if (getCallingActivity().getClassName().equalsIgnoreCase("me.vvander.wander.MyMatches")) {
                Intent intent = getIntent();
                cross = (ArrayList<LatLng>) intent.getSerializableExtra("cross");
            }
        }
    }

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

        if (locationManager != null) {
            Criteria criteria = new Criteria();
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
            if (location != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(17).build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
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

    public void displayHeatmap(View view) {
        if (overlayPersonal == null && !overlayPersonalOn) {
            if (listPersonal.size() > 0) {
                HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder().data(listPersonal).build();
                overlayPersonal = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
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

    public void displayHeatmapAll(View view) {
        int[] colors = {
                Color.rgb(102, 225, 0),
                Color.rgb(255, 0, 0)
        };
        float[] startPoints = {
                0.2f, 1f
        };
        Gradient gradient = new Gradient(colors, startPoints);
        if (overlayAll == null && !overlayAllOn) {
            if (listAll.size() > 0) {
                HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder().data(listAll).gradient(gradient).build();
                overlayAll = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
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
}