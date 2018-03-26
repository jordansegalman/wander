package me.vvander.wander;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;


import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by Kyle on 3/25/2018.
 */

public class DisableTrackingAlarm extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Bundle scheduleBundle = intent.getBundleExtra("Schedule");

        ScheduleItem item = (ScheduleItem) scheduleBundle.get("Schedule");

        disableTracking();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, item.GetStartHour());
        calendar.set(Calendar.MINUTE, item.GetStartMinute());
        calendar.set(Calendar.DAY_OF_WEEK, item.GetNextRepeatAfterToday());

        AlarmManager alarmManager = item.getAlarmManager();
        Intent disable = new Intent(context, DisableTrackingAlarm.class);
        disable.putExtra("ScheduleItem", item);
        PendingIntent pendingDisable = PendingIntent.getBroadcast(context, 0, disable, 0);
        item.setAlarmDisable(pendingDisable);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingDisable);

    }

    private void disableTracking(){
        Data data = Data.getInstance();
        data.setScheduleLocationSwitch(false);
    }
}
