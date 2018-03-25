package me.vvander.wander;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import me.vvander.wander.R;

public class MyMatches extends AppCompatActivity {
    ArrayList<MatchData> matchList;
    ListView matchListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_matches);

        populateList();
        setupListView();

    }

    private void populateList(){
        //TODO: get matches information and put it in matchList
    }

    private void setupListView(){
        String[] listItems;

        if(matchList == null || matchList.isEmpty()){
            listItems = new String[1];
            listItems[0] = "No New Matches";
        }
        else {
            listItems = new String[matchList.size()];

            for (int i = 0; i < matchList.size(); i++) {
                listItems[i] = matchList.get(i).getName();
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.image_list,  listItems);
        matchListView = (ListView) findViewById(R.id.matchesList);
        matchListView.setAdapter(adapter);

        matchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*TODO: when a user clicks on a match in the list this is triggered. The value of position is equal
                to the index of the corresponding matchData in matchList*/

            }
        });
    }
}
