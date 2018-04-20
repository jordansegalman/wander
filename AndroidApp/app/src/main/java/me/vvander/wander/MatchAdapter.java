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

class MatchAdapter extends ArrayAdapter<Match> {
    MatchAdapter(Context context, ArrayList<Match> matchList) {
        super(context, 0, matchList);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.profile_list_item, parent, false);
        }

        Match match = getItem(position);
        if (match != null) {
            TextView name = convertView.findViewById(R.id.name);
            name.setText(match.getName());
            ImageView picture = convertView.findViewById(R.id.picture);
            picture.setImageBitmap(Utilities.decodeImage(match.getPicture()));
        }
        return convertView;
    }
}