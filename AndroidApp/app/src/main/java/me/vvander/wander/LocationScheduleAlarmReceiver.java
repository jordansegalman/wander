package me.vvander.wander;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

public class LocationScheduleAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = LocationScheduleAlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Data.getInstance().getLocationSchedules() != null) {
            Log.d(TAG, "Location schedule alarm broadcast received");
            Data.getInstance().setScheduleLocationSwitch(!locationScheduleActive());
            if (Data.getInstance().getManualLocationSwitch() && Data.getInstance().getScheduleLocationSwitch() && Data.getInstance().getActivityRecognitionLocationSwitch()) {
                context.startService(new Intent(context, LocationCollectionService.class));
            } else {
                context.stopService(new Intent(context, LocationCollectionService.class));
            }
        }
    }

    private boolean locationScheduleActive() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        ArrayList<LocationSchedule> locationSchedules = Data.getInstance().getLocationSchedules();
        if (!locationSchedules.isEmpty()) {
            for (LocationSchedule locationSchedule : locationSchedules) {
                if (locationSchedule.getDays()[day]) {
                    if (hour > locationSchedule.getStartHour()) {
                        if (hour < locationSchedule.getEndHour()) {
                            return true;
                        }
                        if (hour == locationSchedule.getEndHour() && minute <= locationSchedule.getEndMinute()) {
                            return true;
                        }
                    }
                    if (hour == locationSchedule.getStartHour() && minute >= locationSchedule.getStartMinute()) {
                        if (hour < locationSchedule.getEndHour()) {
                            return true;
                        }
                        if (hour == locationSchedule.getEndHour() && minute <= locationSchedule.getEndMinute()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}