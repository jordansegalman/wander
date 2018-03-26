package me.vvander.wander;

import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import me.vvander.wander.R;

public class ScheduleEdit extends AppCompatActivity {
    private EditText nameEdit;
    private Button startButton;
    private Button endButton;
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
    private ScheduleItem item;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_edit);

        nameEdit = (EditText) findViewById(R.id.name);
        startButton = (Button) findViewById(R.id.startTimeButton);
        endButton = (Button) findViewById(R.id.endTimeButton);
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
        position = getIntent().getIntExtra("Position", 0);

        try {
            File listItemsFile = new File(this.getFilesDir(), "ScheduleItems");
            FileInputStream fis = new FileInputStream(listItemsFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            scheduleItems = (ArrayList<ScheduleItem>) ois.readObject();

            ois.close();
            fis.close();

        }
        catch (IOException e){
            e.printStackTrace();
        }
        catch(ClassNotFoundException e){
            e.printStackTrace();
        }

        ScheduleItem currSchedule = scheduleItems.get(position);
        item = currSchedule;

        nameEdit.setText(currSchedule.GetName());
        startHourText.setText(String.valueOf(currSchedule.GetStartHour()));
        startMinuteText.setText(String.valueOf(currSchedule.GetStartMinute()));
        endHourText.setText(String.valueOf(currSchedule.GetEndHour()));
        endMinuteText.setText(String.valueOf(currSchedule.GetEndMinute()));

        if(currSchedule.GetStartMinute() < 10){
            startButton.setText("Start Time: " + currSchedule.GetStartHour() + ":0" + currSchedule.GetStartMinute());
        } else{
            startButton.setText("Start Time: " + currSchedule.GetStartHour() + ":" + currSchedule.GetStartMinute());
        }

        if(currSchedule.GetEndMinute() < 10) {
            endButton.setText("End Time: " + currSchedule.GetEndHour() + ":0" + currSchedule.GetEndMinute());
        } else{
            endButton.setText("End Time: " + currSchedule.GetEndHour() + ":" + currSchedule.GetEndMinute());
        }

        day0.setChecked(currSchedule.GetRepeat()[0]);
        day1.setChecked(currSchedule.GetRepeat()[1]);
        day2.setChecked(currSchedule.GetRepeat()[2]);
        day3.setChecked(currSchedule.GetRepeat()[3]);
        day4.setChecked(currSchedule.GetRepeat()[4]);
        day5.setChecked(currSchedule.GetRepeat()[5]);
        day6.setChecked(currSchedule.GetRepeat()[6]);
    }

    public void done(View view){

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
        }
        catch (IOException e){
            e.printStackTrace();
        }
        catch(ClassNotFoundException e){
            e.printStackTrace();
        }

        item = new ScheduleItem(name, startHour, startMinute, endHour, endMinute, repeatDays);

        scheduleItems.set(position, item);

        try {
            File file = new File(this.getFilesDir(), "ScheduleItems");
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(scheduleItems);

            oos.close();
            fos.close();

        }
        catch (IOException e){
            e.printStackTrace();
        }

        item.resetAlarm(this);

        startActivity(new Intent(ScheduleEdit.this, Schedule.class));
    }

    public void startTimeButton(View view){
        Bundle bundle = new Bundle();
        bundle.putInt("Type", 0);
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(),"TimePicker");
    }

    public void endTimeButton(View view){
        Bundle bundle = new Bundle();
        bundle.putInt("Type", 1);
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(),"TimePicker");
    }

    public void delete(View view){
        item.removeAlarm(this);
        scheduleItems.remove(position);
        try {
            File file = new File(this.getFilesDir(), "ScheduleItems");
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(scheduleItems);

            oos.close();
            fos.close();

        }
        catch (IOException e){
            e.printStackTrace();
        }

        Data data = Data.getInstance();
        if(ScheduleItem.isAnyDisableActive(this)){
            data.setScheduleLocationSwitch(true);
        } else{
            data.setScheduleLocationSwitch(false);
        }

        startActivity(new Intent(ScheduleEdit.this, Schedule.class));
    }

}