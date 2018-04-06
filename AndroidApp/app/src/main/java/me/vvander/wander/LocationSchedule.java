package me.vvander.wander;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Space;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

public class LocationSchedule extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_schedule);
        setupListView();
        updateScheduleLocationSwitch();
    }

    private void setupListView() {
        Space noSchedulesTopSpace = findViewById(R.id.noSchedulesTopSpace);
        TextView noSchedulesText = findViewById(R.id.noSchedulesText);
        Space noSchedulesBottomSpace = findViewById(R.id.noSchedulesBottomSpace);
        ListView scheduleListView = findViewById(R.id.scheduleList);
        ArrayList<LocationScheduleItem> locationSchedule = Data.getInstance().getLocationSchedule();
        if (!locationSchedule.isEmpty()) {
            noSchedulesTopSpace.setVisibility(View.GONE);
            noSchedulesText.setVisibility(View.GONE);
            noSchedulesBottomSpace.setVisibility(View.GONE);
            scheduleListView.setVisibility(View.VISIBLE);
            String[] listItems = new String[locationSchedule.size()];
            for (int i = 0; i < locationSchedule.size(); i++) {
                listItems[i] = locationSchedule.get(i).getName();
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
            scheduleListView.setAdapter(adapter);
            scheduleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(LocationSchedule.this, LocationScheduleEdit.class);
                    intent.putExtra("Position", position);
                    startActivity(intent);
                }
            });
        } else {
            noSchedulesTopSpace.setVisibility(View.VISIBLE);
            noSchedulesText.setVisibility(View.VISIBLE);
            noSchedulesBottomSpace.setVisibility(View.VISIBLE);
            scheduleListView.setVisibility(View.GONE);
            scheduleListView.setEnabled(false);
        }
    }

    private void updateScheduleLocationSwitch() {
        Data.getInstance().setScheduleLocationSwitch(!locationScheduleActive());
        if (Data.getInstance().getManualLocationSwitch() && Data.getInstance().getScheduleLocationSwitch() && Data.getInstance().getActivityRecognitionLocationSwitch()) {
            startService(new Intent(getApplicationContext(), LocationCollectionService.class));
        } else {
            stopService(new Intent(getApplicationContext(), LocationCollectionService.class));
        }
    }

    private boolean locationScheduleActive() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        ArrayList<LocationScheduleItem> locationSchedule = Data.getInstance().getLocationSchedule();
        if (locationSchedule != null && !locationSchedule.isEmpty()) {
            for (LocationScheduleItem locationScheduleItem : locationSchedule) {
                if (locationScheduleItem.getDays()[day] && ((hour > locationScheduleItem.getStartHour() && hour < locationScheduleItem.getEndHour()) || (hour == locationScheduleItem.getStartHour() && minute > locationScheduleItem.getStartMinute()) || (hour == locationScheduleItem.getEndHour() && minute < locationScheduleItem.getEndMinute()))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void newSchedule(View view) {
        startActivity(new Intent(LocationSchedule.this, LocationScheduleNew.class));
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(LocationSchedule.this, Settings.class));
    }
}