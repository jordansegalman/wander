package me.vvander.wander;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.gson.Gson;

import java.util.ArrayList;

public class LocationScheduleEdit extends AppCompatActivity {
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
    private LocationScheduleItem existingSchedule;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_edit);

        nameEdit = findViewById(R.id.name);
        Button startButton = findViewById(R.id.startTimeButton);
        Button endButton = findViewById(R.id.endTimeButton);
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
        position = getIntent().getIntExtra("Position", -1);

        if (position == -1) {
            finish();
        }

        ArrayList<LocationScheduleItem> locationSchedule = Data.getInstance().getLocationSchedule();

        existingSchedule = locationSchedule.get(position);

        nameEdit.setText(existingSchedule.getName());
        startHourText.setText(String.valueOf(existingSchedule.getStartHour()));
        startMinuteText.setText(String.valueOf(existingSchedule.getStartMinute()));
        endHourText.setText(String.valueOf(existingSchedule.getEndHour()));
        endMinuteText.setText(String.valueOf(existingSchedule.getEndMinute()));

        if(existingSchedule.getStartMinute() < 10){
            String startButtonText = "Start Time: " + existingSchedule.getStartHour() + ":0" + existingSchedule.getStartMinute();
            startButton.setText(startButtonText);
        } else{
            String startButtonText = "Start Time: " + existingSchedule.getStartHour() + ":" + existingSchedule.getStartMinute();
            startButton.setText(startButtonText);
        }

        if(existingSchedule.getEndMinute() < 10) {
            String endButtonText = "End Time: " + existingSchedule.getEndHour() + ":0" + existingSchedule.getEndMinute();
            endButton.setText(endButtonText);
        } else{
            String endButtonText = "End Time: " + existingSchedule.getEndHour() + ":" + existingSchedule.getEndMinute();
            endButton.setText(endButtonText);
        }

        day0.setChecked(existingSchedule.getDays()[0]);
        day1.setChecked(existingSchedule.getDays()[1]);
        day2.setChecked(existingSchedule.getDays()[2]);
        day3.setChecked(existingSchedule.getDays()[3]);
        day4.setChecked(existingSchedule.getDays()[4]);
        day5.setChecked(existingSchedule.getDays()[5]);
        day6.setChecked(existingSchedule.getDays()[6]);
    }

    public void done(View view){
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

        existingSchedule = new LocationScheduleItem(name, startHour, startMinute, endHour, endMinute, days);
        locationSchedule.set(position, existingSchedule);

        saveSchedule();

        startActivity(new Intent(LocationScheduleEdit.this, LocationSchedule.class));
    }

    public void startTimeButton(View view){
        Bundle bundle = new Bundle();
        bundle.putString("Time", "Start");
        DialogFragment newFragment = new LocationScheduleTimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(),"TimePicker");
    }

    public void endTimeButton(View view){
        Bundle bundle = new Bundle();
        bundle.putString("Time", "Start");
        DialogFragment newFragment = new LocationScheduleTimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(),"TimePicker");
    }

    public void delete(View view){
        ArrayList<LocationScheduleItem> locationSchedule = Data.getInstance().getLocationSchedule();
        locationSchedule.remove(position);
        saveSchedule();
        startActivity(new Intent(LocationScheduleEdit.this, LocationSchedule.class));
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