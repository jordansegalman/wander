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
        if (Data.getInstance().getLocationSchedule() != null) {
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
        ArrayList<LocationScheduleItem> locationSchedule = Data.getInstance().getLocationSchedule();
        if (!locationSchedule.isEmpty()) {
            for (LocationScheduleItem locationScheduleItem : locationSchedule) {
                if (locationScheduleItem.getDays()[day] && ((hour > locationScheduleItem.getStartHour() && hour < locationScheduleItem.getEndHour()) || (hour == locationScheduleItem.getStartHour() && minute >= locationScheduleItem.getStartMinute()) || (hour == locationScheduleItem.getEndHour() && minute <= locationScheduleItem.getEndMinute()))) {
                    return true;
                }
            }
        }
        return false;
    }
}