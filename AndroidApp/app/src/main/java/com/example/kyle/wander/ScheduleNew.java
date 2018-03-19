package com.example.kyle.wander;

import android.app.DialogFragment;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TimePicker;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class ScheduleNew extends AppCompatActivity {
    private EditText nameEdit;
    private ArrayList<ScheduleItem> scheduleItems = new ArrayList<ScheduleItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_new);

        nameEdit = (EditText) findViewById(R.id.name);
    }

    public void done(View view){

        String name = nameEdit.getText().toString();

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

        scheduleItems.add(new ScheduleItem(name));

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
        startActivity(new Intent(ScheduleNew.this, Schedule.class));
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
}
