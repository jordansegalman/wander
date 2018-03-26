package me.vvander.wander;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class Schedule extends AppCompatActivity {
    private ArrayAdapter<String> adapter;
    private ArrayList<ScheduleItem> scheduleItems = new ArrayList<>();
    private String[] listItems;
    private ListView scheduleListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        setupListView();
    }

    private void setupListView() {
        try {
            File listItemsFile = new File(this.getFilesDir(), "ScheduleItems");
            FileInputStream fis = new FileInputStream(listItemsFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            scheduleItems = (ArrayList<ScheduleItem>) ois.readObject();

            ois.close();
            fis.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (scheduleItems == null || scheduleItems.isEmpty()) {
            listItems = new String[1];
            listItems[0] = "No Schedule Set";
        } else {
            listItems = new String[scheduleItems.size()];

            for (int i = 0; i < scheduleItems.size(); i++) {
                listItems[i] = scheduleItems.get(i).GetName();
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        scheduleListView = findViewById(R.id.scheduleList);
        scheduleListView.setAdapter(adapter);

        scheduleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent edit = new Intent(Schedule.this, ScheduleEdit.class);
                edit.putExtra("Position", position);
                startActivity(edit);

            }
        });
    }

    public void newSchedule(View view) {
        startActivity(new Intent(Schedule.this, ScheduleNew.class));
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(Schedule.this, Settings.class));
    }
}