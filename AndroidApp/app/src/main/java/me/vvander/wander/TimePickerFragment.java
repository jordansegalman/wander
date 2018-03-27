package me.vvander.wander;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

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
        String buttonText;
        TextView hourView;
        TextView minuteView;
        if(minute < 10){
            timeText = hourOfDay + ":0" + minute;
        } else {
            timeText = hourOfDay + ":" + minute;
        }

        if(type == 0){
            buttonText = "Start Time: " + timeText;
            Button startTimeButton = (Button) getActivity().findViewById(R.id.startTimeButton);
            startTimeButton.setText(buttonText);
            hourView = (TextView) getActivity().findViewById(R.id.startHour);
            minuteView = (TextView) getActivity().findViewById(R.id.startMinute);
            hourView.setText(String.valueOf(hourOfDay));
            minuteView.setText(String.valueOf(minute));

        }
        else{
            buttonText = "End Time: " + timeText;
            Button endTimeButton = (Button) getActivity().findViewById(R.id.endTimeButton);
            endTimeButton.setText(buttonText);
            hourView = (TextView) getActivity().findViewById(R.id.endHour);
            minuteView = (TextView) getActivity().findViewById(R.id.endMinute);
            hourView.setText(String.valueOf(hourOfDay));
            minuteView.setText(String.valueOf(minute));
        }
    }
}