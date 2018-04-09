package me.vvander.wander;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class WanderFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            switch (remoteMessage.getData().get("type")) {
                case "New Match": {
                    Log.d("FirebaseMessagingServer", "NEW MATCH");
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        String channel_id = "new_matches_id";
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
                        Intent intent;
                        if (Data.getInstance().getLoggedIn() || Data.getInstance().getLoggedInGoogle() || Data.getInstance().getLoggedInFacebook()) {
                            intent = new Intent(getApplicationContext(), MatchProfileActivity.class);
                            intent.putExtra("uid", remoteMessage.getData().get("uid"));
                            intent.putExtra("name", remoteMessage.getData().get("name"));
                            intent.putExtra("about", remoteMessage.getData().get("about"));
                            intent.putExtra("interests", remoteMessage.getData().get("interests"));
                            intent.putExtra("picture", remoteMessage.getData().get("picture"));
                            intent.putExtra("timesCrossed", remoteMessage.getData().get("timesCrossed"));
                            intent.putExtra("approved", remoteMessage.getData().get("approved"));
                        } else {
                            intent = new Intent(getApplicationContext(), LoginActivity.class);
                        }
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                                .setContentTitle(remoteMessage.getData().get("title"))
                                .setContentText(remoteMessage.getData().get("body"))
                                .setSmallIcon(R.drawable.match_notification_icon)
                                .setAutoCancel(true)
                                .setContentIntent(pendingIntent);
                        notificationManager.notify(1, notificationBuilder.build());
                    }
                    break;
                }
                case "Crossed Paths": {
                    Log.d("FirebaseMessagingServer", "CROSSED PATHS");
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        String channel_id = "crossed_paths_id";
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
                        Intent intent;
                        if (Data.getInstance().getLoggedIn() || Data.getInstance().getLoggedInGoogle() || Data.getInstance().getLoggedInFacebook()) {
                            intent = new Intent(getApplicationContext(), MatchProfileActivity.class);
                            intent.putExtra("uid", remoteMessage.getData().get("uid"));
                            intent.putExtra("name", remoteMessage.getData().get("name"));
                            intent.putExtra("about", remoteMessage.getData().get("about"));
                            intent.putExtra("interests", remoteMessage.getData().get("interests"));
                            intent.putExtra("picture", remoteMessage.getData().get("picture"));
                            intent.putExtra("timesCrossed", remoteMessage.getData().get("timesCrossed"));
                            intent.putExtra("approved", remoteMessage.getData().get("approved"));
                        } else {
                            intent = new Intent(getApplicationContext(), LoginActivity.class);
                        }
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                                .setContentTitle(remoteMessage.getData().get("title"))
                                .setContentText(remoteMessage.getData().get("body"))
                                .setSmallIcon(R.drawable.cross_notification_icon)
                                .setAutoCancel(true)
                                .setContentIntent(pendingIntent);
                        notificationManager.notify(1, notificationBuilder.build());
                    }
                    break;
                }
                case "Chat Message": {
                    Log.d("FirebaseMessagingServer", "CHAT MESSAGE");
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        String channel_id = "chat_messages_id";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            CharSequence channel_name = "Chat Messages";
                            int importance = NotificationManager.IMPORTANCE_HIGH;
                            NotificationChannel notificationChannel = new NotificationChannel(channel_id, channel_name, importance);
                            notificationChannel.enableLights(true);
                            notificationChannel.setLightColor(R.color.colorAccent);
                            notificationChannel.enableVibration(true);
                            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 200, 100});
                            notificationManager.createNotificationChannel(notificationChannel);
                        }
                        Intent intent;
                        if (Data.getInstance().getLoggedIn() || Data.getInstance().getLoggedInGoogle() || Data.getInstance().getLoggedInFacebook()) {
                            intent = new Intent(getApplicationContext(), ChatActivity.class);
                            intent.putExtra("UID", remoteMessage.getData().get("uid"));
                            intent.putExtra("name", remoteMessage.getData().get("title"));
                        } else {
                            intent = new Intent(getApplicationContext(), LoginActivity.class);
                        }
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                                .setContentTitle(remoteMessage.getData().get("title"))
                                .setContentText(remoteMessage.getData().get("body"))
                                .setSmallIcon(R.drawable.message_notification_icon)
                                .setAutoCancel(true)
                                .setContentIntent(pendingIntent);
                        notificationManager.notify(1, notificationBuilder.build());
                    }
                    break;
                }
            }
        }
    }
}