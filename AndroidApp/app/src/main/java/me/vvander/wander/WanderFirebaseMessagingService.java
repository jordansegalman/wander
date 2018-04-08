package me.vvander.wander;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class WanderFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().get("title").equals("You have a new match!")) {
                String channel_id = "new_matches_id";
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        CharSequence channel_name = "New Matches";
                        int importance = NotificationManager.IMPORTANCE_HIGH;
                        NotificationChannel notificationChannel = new NotificationChannel(channel_id, channel_name, importance);
                        notificationChannel.enableLights(true);
                        notificationChannel.setLightColor(R.color.colorAccent);
                        notificationChannel.enableVibration(true);
                        notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 200, 100});
                        notificationManager.createNotificationChannel(notificationChannel);
                    }
                    Intent intent = new Intent(getApplicationContext(), MatchesActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                            .setContentTitle(remoteMessage.getData().get("title"))
                            .setContentText(remoteMessage.getData().get("body"))
                            .setSmallIcon(R.drawable.match_notification_icon)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);
                    if(Data.getInstance().getNotificationStatus())
                    notificationManager.notify(1, notificationBuilder.build());
                }
            } else if (remoteMessage.getData().get("title").equals("You just crossed paths with one of your matches!")) {
                String channel_id = "crossed_paths_id";
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        CharSequence channel_name = "Crossed Paths";
                        int importance = NotificationManager.IMPORTANCE_HIGH;
                        NotificationChannel notificationChannel = new NotificationChannel(channel_id, channel_name, importance);
                        notificationChannel.enableLights(true);
                        notificationChannel.setLightColor(R.color.colorAccent);
                        notificationChannel.enableVibration(true);
                        notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 200, 100});
                        notificationManager.createNotificationChannel(notificationChannel);
                    }
                    Intent intent = new Intent(getApplicationContext(), MatchesActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                            .setContentTitle(remoteMessage.getData().get("title"))
                            .setContentText(remoteMessage.getData().get("body"))
                            .setSmallIcon(R.drawable.cross_notification_icon)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);
                    if(Data.getInstance().getNotificationStatus())
                    notificationManager.notify(1, notificationBuilder.build());
                }
            }
        }
    }
}