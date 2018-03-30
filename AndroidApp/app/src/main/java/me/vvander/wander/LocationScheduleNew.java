package me.vvander.wander;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;

import java.util.ArrayList;

public class LocationScheduleNew extends AppCompatActivity {
    private static final String SP_SCHEDULE = "locationSchedule";
    private EditText nameEdit;
    private TextView startHourText;
    private TextView startMinuteText;
    private TextView endHourText;
    private TextView endMinuteText;
    private ToggleButton day0;
    private ToggleButton day1;
    private ToggleButton day2;
    private ToggleButton day3;
    private ToggleButton day4;
    private ToggleButton day5;
    private ToggleButton day6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_schedule_new);

        nameEdit = findViewById(R.id.name);
        startHourText = findViewById(R.id.startHour);
        startMinuteText = findViewById(R.id.startMinute);
        endHourText = findViewById(R.id.endHour);
        endMinuteText = findViewById(R.id.endMinute);
        day0 = findViewById(R.id.day0);
        day1 = findViewById(R.id.day1);
        day2 = findViewById(R.id.day2);
        day3 = findViewById(R.id.day3);
        day4 = findViewById(R.id.day4);
        day5 = findViewById(R.id.day5);
        day6 = findViewById(R.id.day6);
    }

    public void done(View view) {
        if (nameEdit.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Enter a schedule name.", Toast.LENGTH_SHORT).show();
        } else if (startHourText.getText().toString().isEmpty() || startMinuteText.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Choose a start time.", Toast.LENGTH_SHORT).show();
        } else if (endHourText.getText().toString().isEmpty() || endMinuteText.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Choose an end time.", Toast.LENGTH_SHORT).show();
        } else if (!day0.isChecked() && !day1.isChecked() && !day2.isChecked() && !day3.isChecked() && !day4.isChecked() && !day5.isChecked() && !day6.isChecked()) {
            Toast.makeText(getApplicationContext(), "Select at least one day.", Toast.LENGTH_SHORT).show();
        } else {
            String name = nameEdit.getText().toString();
            int startHour = Integer.parseInt(startHourText.getText().toString());
            int startMinute = Integer.parseInt(startMinuteText.getText().toString());
            int endHour = Integer.parseInt(endHourText.getText().toString());
            int endMinute = Integer.parseInt(endMinuteText.getText().toString());
            boolean[] days = new boolean[7];
            days[0] = day0.isChecked();
            days[1] = day1.isChecked();
            days[2] = day2.isChecked();
            days[3] = day3.isChecked();
            days[4] = day4.isChecked();
            days[5] = day5.isChecked();
            days[6] = day6.isChecked();

            ArrayList<LocationScheduleItem> locationSchedule = Data.getInstance().getLocationSchedule();

            LocationScheduleItem newSchedule = new LocationScheduleItem(name, startHour, startMinute, endHour, endMinute, days);
            locationSchedule.add(newSchedule);

            saveSchedule();

            startActivity(new Intent(LocationScheduleNew.this, LocationSchedule.class));
        }
    }

    public void startTimeButton(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("Time", "Start");
        DialogFragment newFragment = new LocationScheduleTimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(), "TimePicker");
    }

    public void endTimeButton(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("Time", "End");
        DialogFragment newFragment = new LocationScheduleTimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(), "TimePicker");
    }

    private void saveSchedule() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_SCHEDULE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(Data.getInstance().getLocationSchedule());
        editor.putString("schedule", json);
        editor.apply();
    }
}