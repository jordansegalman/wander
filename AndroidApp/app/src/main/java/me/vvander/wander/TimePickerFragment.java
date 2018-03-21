package me.vvander.wander;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TimePicker;

import me.vvander.wander.R;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    private int type; //0 = start time, 1 = end time

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        type = getArguments().getInt("Type");

        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        String timeText;
        if(minute < 10){
            timeText = hourOfDay + ":0" + minute;
        }
        else{
            timeText = hourOfDay + ":" + minute;
        }


        if(type == 0){
            Button startTimeButton = (Button) getActivity().findViewById(R.id.startTimeButton);
            startTimeButton.setText(timeText);
        }
        else{
            Button endTimeButton = (Button) getActivity().findViewById(R.id.endTimeButton);
            endTimeButton.setText(timeText);
        }
    }
}