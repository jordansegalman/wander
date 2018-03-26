package me.vvander.wander;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by Kyle on 3/25/2018.
 */

public class EnableTrackingAlarm {
    public void onReceive(final Context context, Intent intent) {
        Bundle scheduleBundle = intent.getBundleExtra("Schedule");

        ScheduleItem item = (ScheduleItem) scheduleBundle.get("Schedule");

        enableTracking(context);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, item.GetEndHour());
        calendar.set(Calendar.MINUTE, item.GetEndMinute());
        calendar.set(Calendar.DAY_OF_WEEK, item.GetNextRepeatAfterToday());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        PendingIntent pendingEnable = item.getAlarmEnable(context);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingEnable);
    }

    private void enableTracking(Context context){
        if(!ScheduleItem.isAnyDisableActive(context)) {
            Data data = Data.getInstance();
            data.setScheduleLocationSwitch(true);
        }
    }
}
