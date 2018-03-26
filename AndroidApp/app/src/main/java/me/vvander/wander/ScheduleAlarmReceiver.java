package me.vvander.wander;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 * Created by Kyle on 3/26/2018.
 */

public class ScheduleAlarmReceiver {

    public void onReceive(Context context, Intent intent) {
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

        Data.getInstance().setScheduleLocationSwitch(true);

        for (ScheduleItem item : scheduleItems) {
            item.resetAlarm(context);
        }
    }
}
