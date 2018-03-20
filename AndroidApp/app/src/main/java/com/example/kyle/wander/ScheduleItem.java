package com.example.kyle.wander;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ScheduleItem implements Serializable{
    private String name;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;
    private boolean[] repeatDays = new boolean[7]; //Starts with sunday

    public ScheduleItem(String name){
        this.name = name;
    }

    public void SetName(String name){this.name = name;}
    public void SetStartHour(int startHour) {this.startHour = startHour;}
    public void SetStartMinute(int startMinute) {this.startMinute = startMinute;}
    public void SetEndHour(int endHour) {this.endHour = endHour;}
    public void SetEndMinute(int endMinute) {this.endMinute = endMinute;}
    public void SetRepeat(boolean[] days) {repeatDays = days;}

    public String GetName() {return name;}
    public int GetStartHour() {return startHour;}
    public int GetStartMinute() {return startMinute;}
    public int GetEndHour() {return endHour;}
    public int GetEndMinute() {return endMinute;}
    public boolean[] GetRepeat() {return repeatDays;}

    public int GetNextRepeat() {

        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 1;

        for(int i = 0; i < 7; i++){
            if(repeatDays[day] == true){
                return day;
            }
            day = (day == 6 ? 0 : day + 1);
        }
        return -1;
    }
}