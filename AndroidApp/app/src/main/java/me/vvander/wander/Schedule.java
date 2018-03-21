package me.vvander.wander;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import me.vvander.wander.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class Schedule extends AppCompatActivity {
    private ArrayAdapter<String> adapter;
    private ArrayList<ScheduleItem> scheduleItems = new ArrayList<ScheduleItem>();
    private String[] listItems;
    private ListView scheduleListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        setupListView();
    }

    private void setupListView(){
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

        if(scheduleItems == null || scheduleItems.isEmpty()){
            listItems = new String[1];
            listItems[0] = "No Schedule Set";
        }
        else {
            listItems = new String[scheduleItems.size()];

            for (int i = 0; i < scheduleItems.size(); i++) {
                listItems[i] = scheduleItems.get(i).GetName();
            }
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        scheduleListView = (ListView) findViewById(R.id.scheduleList);
        scheduleListView.setAdapter(adapter);
    }

    public void newSchedule(View view){
        startActivity(new Intent(Schedule.this, ScheduleNew.class));
    }
}