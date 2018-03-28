package me.vvander.wander;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class LocationScheduleTimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    private String time;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        time = getArguments().getString("Time");
        return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        String timeText;
        String buttonText;
        TextView startHourView = getActivity().findViewById(R.id.startHour);
        TextView startMinuteView = getActivity().findViewById(R.id.startMinute);
        TextView endHourView = getActivity().findViewById(R.id.endHour);
        TextView endMinuteView = getActivity().findViewById(R.id.endMinute);
        if (hourOfDay < 12) {
            if(minute < 10){
                timeText = hourOfDay % 12 + ":0" + minute + " AM";
            } else {
                timeText = hourOfDay % 12 + ":" + minute + " AM";
            }
        } else {
            if(minute < 10){
                timeText = hourOfDay % 12 + ":0" + minute + " PM";
            } else {
                timeText = hourOfDay % 12 + ":" + minute + " PM";
            }
        }
        if (time.equals("Start")) {
            if (!endHourView.getText().toString().isEmpty() && !endMinuteView.getText().toString().isEmpty()) {
                int endHour = Integer.parseInt(endHourView.getText().toString());
                int endMinute = Integer.parseInt(endMinuteView.getText().toString());
                if (endHour < hourOfDay || (endHour == hourOfDay && endMinute <= minute)) {
                    Toast.makeText(getContext(), "Start time must be before end time.", Toast.LENGTH_SHORT).show();
                } else {
                    startHourView.setText(String.valueOf(hourOfDay));
                    startMinuteView.setText(String.valueOf(minute));
                    buttonText = "Start Time: " + timeText;
                    Button startTimeButton = getActivity().findViewById(R.id.startTimeButton);
                    startTimeButton.setText(buttonText);
                }
            } else {
                startHourView.setText(String.valueOf(hourOfDay));
                startMinuteView.setText(String.valueOf(minute));
                buttonText = "Start Time: " + timeText;
                Button startTimeButton = getActivity().findViewById(R.id.startTimeButton);
                startTimeButton.setText(buttonText);
            }
        } else if (time.equals("End")) {
            if (!startHourView.getText().toString().isEmpty() && !startMinuteView.getText().toString().isEmpty()) {
                int startHour = Integer.parseInt(startHourView.getText().toString());
                int startMinute = Integer.parseInt(startMinuteView.getText().toString());
                if (startHour > hourOfDay || (startHour == hourOfDay && startMinute >= minute)) {
                    Toast.makeText(getContext(), "End time must be after start time.", Toast.LENGTH_SHORT).show();
                } else {
                    endHourView.setText(String.valueOf(hourOfDay));
                    endMinuteView.setText(String.valueOf(minute));
                    buttonText = "End Time: " + timeText;
                    Button endTimeButton = getActivity().findViewById(R.id.endTimeButton);
                    endTimeButton.setText(buttonText);
                }
            } else {
                endHourView.setText(String.valueOf(hourOfDay));
                endMinuteView.setText(String.valueOf(minute));
                buttonText = "End Time: " + timeText;
                Button endTimeButton = getActivity().findViewById(R.id.endTimeButton);
                endTimeButton.setText(buttonText);
            }
        }
    }
}