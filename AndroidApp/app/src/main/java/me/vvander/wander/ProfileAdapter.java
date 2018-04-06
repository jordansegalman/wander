package me.vvander.wander;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ProfileAdapter extends ArrayAdapter<MatchData> {
    ProfileAdapter(Context context, ArrayList<MatchData> matchList) {
        super(context, 0, matchList);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.match_list_item, parent, false);
        }

        MatchData matchData = getItem(position);
        if (matchData != null) {
            TextView name = convertView.findViewById(R.id.name);
            name.setText(matchData.getName());
            ImageView picture = convertView.findViewById(R.id.picture);
            picture.setImageBitmap(Utilities.decodeImage(matchData.getPicture()));
        }
        return convertView;
    }
}