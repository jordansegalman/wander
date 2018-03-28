package me.vvander.wander;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class AppHome extends AppCompatActivity {
    private static final String SP_LOCATION = "locationSwitch";
    private static final String SP_SCHEDULE = "locationSchedule";
    private static final int ACTIVITY_RECOGNITION_DETECTION_INTERVAL = 15000;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_home);

        initializeManualLocationSwitch();
        initializeScheduleLocationSwitch();
        setupLocationScheduleAlarm();
        setupActivityRecognition();

        mDrawerList = findViewById(R.id.navList);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerLayout = findViewById(R.id.drawer_layout);

        addDrawerItems();
        setupDrawer();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
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
            Data.getInstance().setLocationSchedule((ArrayList<LocationScheduleItem>) gson.fromJson(json, new TypeToken<ArrayList<LocationScheduleItem>>() {
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

    private void addDrawerItems() {
        String[] osArray = {"My Matches", "Profile", "Popular Locations", "History", "Settings", "Map"};
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    startActivity(new Intent(AppHome.this, MyMatches.class));
                }

                if (position == 1) {
                    startActivity(new Intent(AppHome.this, Profile.class));
                }

                if (position == 4) {
                    startActivity(new Intent(AppHome.this, Settings.class));
                }

                if (position == 5) {
                    startActivity(new Intent(AppHome.this, Map.class));
                }
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return item.getItemId() == R.id.action_settings || mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
}