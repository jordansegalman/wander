package me.vvander.wander;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;

public class ScheduleItem implements Serializable {
    private String name;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;
    private boolean[] repeatDays = new boolean[7]; //Starts with sunday

    public ScheduleItem(String name, int startHour, int startMinute, int endHour, int endMinute, boolean[] repeatDays){

        this.name = name;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.repeatDays = repeatDays;
    }

    public void SetName(String name) {
        this.name = name;
    }

    public void SetStartHour(int startHour) {
        this.startHour = startHour;
    }

    public void SetStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public void SetEndHour(int endHour) {
        this.endHour = endHour;
    }

    public void SetEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    public void SetRepeat(boolean[] days) {
        repeatDays = days;
    }

    public String GetName() {
        return name;
    }

    public int GetStartHour() {
        return startHour;
    }

    public int GetStartMinute() {
        return startMinute;
    }

    public int GetEndHour() {
        return endHour;
    }

    public int GetEndMinute() {
        return endMinute;
    }

    public boolean[] GetRepeat() {
        return repeatDays;
    }

    public PendingIntent getAlarmDisable(Context context) {
        Intent disable = new Intent(context, DisableTrackingAlarm.class);
        disable.putExtra("ScheduleItem", this);
        PendingIntent pendingDisable = PendingIntent.getBroadcast(context, 0, disable, 0);

        return pendingDisable;
    }

    public PendingIntent getAlarmEnable(Context context) {
        Intent enable = new Intent(context, EnableTrackingAlarm.class);
        enable.putExtra("ScheduleItem", this);
        PendingIntent pendingEnable = PendingIntent.getBroadcast(context, 0, enable, 0);

        return pendingEnable;
    }

    public int GetNextRepeat() {

        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;

        for (int i = 0; i < 7; i++) {
            if (repeatDays[day]) {
                return day + 1;
            }
            day = (day == 6 ? 0 : day + 1);
        }
        return -1;
    }

    public int GetNextRepeatAfterToday() {

        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

        for (int i = 0; i < 7; i++) {
            if (repeatDays[day]) {
                return day + 1;
            }
            day = (day == 6 ? 0 : day + 1);
        }
        return -1;
    }

    public void resetAlarm(Context context){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        Data data = Data.getInstance();
        if(isDisableActive()){
            data.setScheduleLocationSwitch(true);
        } else{
            data.setScheduleLocationSwitch(false);
        }

        PendingIntent pendingDisable = getAlarmDisable(context);
        PendingIntent pendingEnable = getAlarmEnable(context);

        alarmManager.cancel(pendingEnable);
        alarmManager.cancel(pendingDisable);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, startMinute);
        calendar.set(Calendar.DAY_OF_WEEK, GetNextRepeat());

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingDisable);

        calendar.set(Calendar.HOUR_OF_DAY, endHour);
        calendar.set(Calendar.MINUTE, endMinute);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingEnable);
    }

    public boolean isDisableActive(){
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        if(repeatDays[day] && hour < endHour && hour > startHour && minute < endMinute && minute > startMinute){
            return true;
        }
        return false;
    }

    public static boolean isAnyDisableActive(Context context){
        ArrayList<ScheduleItem> scheduleItems = null;

        try {
            File listItemsFile = new File(context.getFilesDir(), "ScheduleItems");
            FileInputStream fis = new FileInputStream(listItemsFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            scheduleItems = (ArrayList<ScheduleItem>) ois.readObject();

            ois.close();
            fis.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        for(ScheduleItem item : scheduleItems){
            if(item.isDisableActive()){
                return true;
            }
        }
        return false;
    }

    public void removeAlarm(Context context){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        alarmManager.cancel(getAlarmEnable(context));
        alarmManager.cancel(getAlarmDisable(context));
    }

}