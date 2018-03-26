package me.vvander.wander;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

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

        Data data = Data.getInstance();
        if(isDisableActive()){
            data.setScheduleLocationSwitch(false);
        } else{
            data.setScheduleLocationSwitch(true);
        }

    }

    public void resetAlarm(){

        Data data = Data.getInstance();
        if(isDisableActive()){
            data.setScheduleLocationSwitch(false);
        } else{
            data.setScheduleLocationSwitch(true);
        }

    }

    public boolean isDisableActive(){
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        if(repeatDays[day] && hour < endHour && hour > startHour){
            return true;
        }
        if(repeatDays[day] && hour == startHour && minute > startMinute){
            return true;
        }
        if(repeatDays[day] && hour == endHour && minute < endMinute){
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

}