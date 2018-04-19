package me.vvander.wander;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;

import java.util.ArrayList;

public class LocationScheduleEditActivity extends AppCompatActivity {
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
    private LocationSchedule existingSchedule;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_schedule_edit);

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

        ArrayList<LocationSchedule> locationSchedule = Data.getInstance().getLocationSchedules();

        existingSchedule = locationSchedule.get(position);

        nameEdit.setText(existingSchedule.getName());
        startHourText.setText(String.valueOf(existingSchedule.getStartHour()));
        startMinuteText.setText(String.valueOf(existingSchedule.getStartMinute()));
        endHourText.setText(String.valueOf(existingSchedule.getEndHour()));
        endMinuteText.setText(String.valueOf(existingSchedule.getEndMinute()));

        String timeText;
        if (existingSchedule.getStartHour() < 12) {
            if (existingSchedule.getStartHour() == 0) {
                if (existingSchedule.getStartMinute() < 10) {
                    timeText = existingSchedule.getStartHour() + 12 + ":0" + existingSchedule.getStartMinute() + " AM";
                } else {
                    timeText = existingSchedule.getStartHour() + 12 + ":" + existingSchedule.getStartMinute() + " AM";
                }
            } else {
                if (existingSchedule.getStartMinute() < 10) {
                    timeText = existingSchedule.getStartHour() % 12 + ":0" + existingSchedule.getStartMinute() + " AM";
                } else {
                    timeText = existingSchedule.getStartHour() % 12 + ":" + existingSchedule.getStartMinute() + " AM";
                }
            }
        } else {
            if (existingSchedule.getStartHour() == 12) {
                if (existingSchedule.getStartMinute() < 10) {
                    timeText = existingSchedule.getStartHour() + ":0" + existingSchedule.getStartMinute() + " PM";
                } else {
                    timeText = existingSchedule.getStartHour() + ":" + existingSchedule.getStartMinute() + " PM";
                }
            } else {
                if (existingSchedule.getStartMinute() < 10) {
                    timeText = existingSchedule.getStartHour() % 12 + ":0" + existingSchedule.getStartMinute() + " PM";
                } else {
                    timeText = existingSchedule.getStartHour() % 12 + ":" + existingSchedule.getStartMinute() + " PM";
                }
            }
        }
        String startButtonText = "Start Time: " + timeText;
        startButton.setText(startButtonText);
        if (existingSchedule.getEndHour() < 12) {
            if (existingSchedule.getEndHour() == 0) {
                if (existingSchedule.getEndMinute() < 10) {
                    timeText = existingSchedule.getEndHour() + 12 + ":0" + existingSchedule.getEndMinute() + " AM";
                } else {
                    timeText = existingSchedule.getEndHour() + 12 + ":" + existingSchedule.getEndMinute() + " AM";
                }
            } else {
                if (existingSchedule.getEndMinute() < 10) {
                    timeText = existingSchedule.getEndHour() % 12 + ":0" + existingSchedule.getEndMinute() + " AM";
                } else {
                    timeText = existingSchedule.getEndHour() % 12 + ":" + existingSchedule.getEndMinute() + " AM";
                }
            }
        } else {
            if (existingSchedule.getEndHour() == 12) {
                if (existingSchedule.getEndMinute() < 10) {
                    timeText = existingSchedule.getEndHour() + ":0" + existingSchedule.getEndMinute() + " PM";
                } else {
                    timeText = existingSchedule.getEndHour() + ":" + existingSchedule.getEndMinute() + " PM";
                }
            } else {
                if (existingSchedule.getEndMinute() < 10) {
                    timeText = existingSchedule.getEndHour() % 12 + ":0" + existingSchedule.getEndMinute() + " PM";
                } else {
                    timeText = existingSchedule.getEndHour() % 12 + ":" + existingSchedule.getEndMinute() + " PM";
                }
            }
        }
        String endButtonText = "End Time: " + timeText;
        endButton.setText(endButtonText);

        day0.setChecked(existingSchedule.getDays()[0]);
        day1.setChecked(existingSchedule.getDays()[1]);
        day2.setChecked(existingSchedule.getDays()[2]);
        day3.setChecked(existingSchedule.getDays()[3]);
        day4.setChecked(existingSchedule.getDays()[4]);
        day5.setChecked(existingSchedule.getDays()[5]);
        day6.setChecked(existingSchedule.getDays()[6]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location_schedule_edit_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_schedule:
                delete();
                return true;
        }
        return super.onOptionsItemSelected(item);
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

            ArrayList<LocationSchedule> locationSchedule = Data.getInstance().getLocationSchedules();

            existingSchedule = new LocationSchedule(name, startHour, startMinute, endHour, endMinute, days);
            locationSchedule.set(position, existingSchedule);

            saveSchedule();

            startActivity(new Intent(LocationScheduleEditActivity.this, LocationScheduleActivity.class));
            finish();
        }
    }

    public void chooseStartTime(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("Time", "Start");
        DialogFragment newFragment = new LocationScheduleTimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(), "TimePicker");
    }

    public void chooseEndTime(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("Time", "End");
        DialogFragment newFragment = new LocationScheduleTimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(), "TimePicker");
    }

    private void delete() {
        ArrayList<LocationSchedule> locationSchedule = Data.getInstance().getLocationSchedules();
        locationSchedule.remove(position);
        saveSchedule();
        startActivity(new Intent(LocationScheduleEditActivity.this, LocationScheduleActivity.class));
        finish();
    }

    private void saveSchedule() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_SCHEDULE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(Data.getInstance().getLocationSchedules());
        editor.putString("schedule", json);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(LocationScheduleEditActivity.this, LocationScheduleActivity.class));
        finish();
    }
}