package me.vvander.wander;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ProfileAdapter extends ArrayAdapter<MatchData> {

    public ProfileAdapter(Context context, ArrayList<MatchData> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.profile_list, parent, false);
        }

        MatchData matchData = getItem(position);
        if (matchData != null) {
            TextView name = (TextView) convertView.findViewById(R.id.name);
            name.setText(matchData.getName());
        }
        return convertView;
    }
}
