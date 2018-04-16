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

public class BlockedAdapter extends ArrayAdapter<Blocked> {
    BlockedAdapter(Context context, ArrayList<Blocked> blockedList) {
        super(context, 0, blockedList);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.profile_list_item, parent, false);
        }

        Blocked blocked = getItem(position);
        if (blocked != null) {
            TextView name = convertView.findViewById(R.id.name);
            name.setText(blocked.getName());
            ImageView picture = convertView.findViewById(R.id.picture);
            picture.setImageBitmap(Utilities.decodeImage(blocked.getPicture()));
        }
        return convertView;
    }
}