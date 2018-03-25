package me.vvander.wander;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.BatchUpdateException;
import java.util.ArrayList;

public class ScheduleNew extends AppCompatActivity {
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
    private ArrayList<ScheduleItem> scheduleItems = new ArrayList<ScheduleItem>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_new);

        nameEdit = (EditText) findViewById(R.id.name);
        startHourText = (TextView) findViewById(R.id.startHour);
        startMinuteText = (TextView) findViewById(R.id.startMinute);
        endHourText = (TextView) findViewById(R.id.endHour);
        endMinuteText = (TextView) findViewById(R.id.endMinute);
        day0 = (ToggleButton) findViewById(R.id.day0);
        day1 = (ToggleButton) findViewById(R.id.day1);
        day2 = (ToggleButton) findViewById(R.id.day2);
        day3 = (ToggleButton) findViewById(R.id.day3);
        day4 = (ToggleButton) findViewById(R.id.day4);
        day5 = (ToggleButton) findViewById(R.id.day5);
        day6 = (ToggleButton) findViewById(R.id.day6);

    }

    public void done(View view) {

        String name = nameEdit.getText().toString();
        int startHour = Integer.parseInt(startHourText.getText().toString());
        int startMinute = Integer.parseInt(startMinuteText.getText().toString());
        int endHour = Integer.parseInt(endHourText.getText().toString());
        int endMinute = Integer.parseInt(endMinuteText.getText().toString());
        boolean[] repeatDays = new boolean[7];
        repeatDays[0] = day0.isChecked();
        repeatDays[1] = day1.isChecked();
        repeatDays[2] = day2.isChecked();
        repeatDays[3] = day3.isChecked();
        repeatDays[4] = day4.isChecked();
        repeatDays[5] = day5.isChecked();
        repeatDays[6] = day6.isChecked();

        try {
            File listItemsFile = new File(this.getFilesDir(), "ScheduleItems");
            FileInputStream fis = new FileInputStream(listItemsFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            scheduleItems = (ArrayList<ScheduleItem>) ois.readObject();

            ois.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        scheduleItems.add(new ScheduleItem(name, startHour, startMinute, endHour, endMinute, repeatDays));

        try {
            File file = new File(this.getFilesDir(), "ScheduleItems");
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(scheduleItems);

            oos.close();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        startActivity(new Intent(ScheduleNew.this, Schedule.class));
    }

    public void startTimeButton(View view) {
        Bundle bundle = new Bundle();
        bundle.putInt("Type", 0);
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(), "TimePicker");
    }

    public void endTimeButton(View view) {
        Bundle bundle = new Bundle();
        bundle.putInt("Type", 1);
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(), "TimePicker");
    }
}