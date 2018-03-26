package me.vvander.wander;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Calendar;

/**
 * Created by Kyle on 3/25/2018.
 */

public class EnableTrackingAlarm {
    public void onReceive(final Context context, Intent intent) {
        Bundle scheduleBundle = intent.getBundleExtra("Schedule");

        ScheduleItem item = (ScheduleItem) scheduleBundle.get("Schedule");

        enableTracking();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, item.GetEndHour());
        calendar.set(Calendar.MINUTE, item.GetEndMinute());
        calendar.set(Calendar.DAY_OF_WEEK, item.GetNextRepeatAfterToday());

        AlarmManager alarmManager = item.getAlarmManager();
        Intent enable = new Intent(context, EnableTrackingAlarm.class);
        enable.putExtra("ScheduleItem", item);
        PendingIntent pendingEnable = PendingIntent.getBroadcast(context, 0, enable, 0);
        item.setAlarmDisable(pendingEnable);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingEnable);
    }

    private void enableTracking(){
        Data data = Data.getInstance();
        data.setScheduleLocationSwitch(true);
    }
}
