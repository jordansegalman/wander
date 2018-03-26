package me.vvander.wander;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

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
            CircleImageView circleImageView = (CircleImageView) convertView.findViewById(R.id.picture);
            if (matchData.getPicture() != null) {
                byte[] decoded_string = Base64.decode(matchData.getPicture(), Base64.DEFAULT);
                if (decoded_string == null) {
                    Log.d("TAG", "ERROR!");
                }
                Bitmap decoded_byte = BitmapFactory.decodeByteArray(decoded_string, 0, decoded_string.length);
                circleImageView.setImageBitmap(decoded_byte);
            }
        }
        return convertView;
    }
}
