package me.vvander.wander;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WanderFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = WanderFirebaseMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (Data.getInstance().getNotificationSwitch() && remoteMessage.getData().size() > 0) {
            switch (remoteMessage.getData().get("type")) {
                case "New Matches": {
                    if (remoteMessage.getData().get("uid").isEmpty()) {
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
                                intent = new Intent(getApplicationContext(), MatchesActivity.class);
                            } else {
                                intent = new Intent(getApplicationContext(), LoginActivity.class);
                            }
                            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                                    .setContentTitle(remoteMessage.getData().get("title"))
                                    .setContentText(remoteMessage.getData().get("body"))
                                    .setSmallIcon(R.drawable.match_notification_icon)
                                    .setAutoCancel(true)
                                    .setContentIntent(pendingIntent);
                            notificationManager.notify(1, notificationBuilder.build());
                        }
                    } else {
                        setupMatchProfileIntent(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"), remoteMessage.getData().get("uid"), "new_matches_id", "New Matches", R.drawable.match_notification_icon);
                    }
                    break;
                }
                case "Crossed Paths": {
                    setupMatchProfileIntent(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"), remoteMessage.getData().get("uid"), "crossed_paths_id", "Crossed Paths", R.drawable.cross_notification_icon);
                    break;
                }
                case "Match Approval": {
                    setupMatchProfileIntent(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"), remoteMessage.getData().get("uid"), "match_approvals_id", "Match Approvals", R.drawable.approval_notification_icon);
                    break;
                }
                case "Shared Interests": {
                    setupMatchProfileIntent(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"), remoteMessage.getData().get("uid"), "shared_interests_id", "Shared Interests", R.drawable.interests_notification_icon);
                    break;
                }
                case "Chat Message": {
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
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
                case "Location Suggestions": {
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        String channel_id = "location_suggestions_id";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            CharSequence channel_name = "Location Suggestions";
                            int importance = NotificationManager.IMPORTANCE_HIGH;
                            NotificationChannel notificationChannel = new NotificationChannel(channel_id, channel_name, importance);
                            notificationChannel.enableLights(true);
                            notificationChannel.setLightColor(R.color.colorAccent);
                            notificationChannel.enableVibration(true);
                            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 200, 100});
                            notificationManager.createNotificationChannel(notificationChannel);
                        }
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                                .setContentTitle(remoteMessage.getData().get("title"))
                                .setContentText(remoteMessage.getData().get("body"))
                                .setSmallIcon(R.drawable.suggestion_notification_icon)
                                .setAutoCancel(true);
                        notificationManager.notify(1, notificationBuilder.build());
                    }
                    break;
                }
                case "Offense Warning": {
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        String channel_id = "offense_warnings_id";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            CharSequence channel_name = "Offense Warnings";
                            int importance = NotificationManager.IMPORTANCE_HIGH;
                            NotificationChannel notificationChannel = new NotificationChannel(channel_id, channel_name, importance);
                            notificationChannel.enableLights(true);
                            notificationChannel.setLightColor(R.color.colorAccent);
                            notificationChannel.enableVibration(true);
                            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 200, 100});
                            notificationManager.createNotificationChannel(notificationChannel);
                        }
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                                .setContentTitle(remoteMessage.getData().get("title"))
                                .setContentText(remoteMessage.getData().get("body"))
                                .setSmallIcon(R.drawable.offense_notification_icon)
                                .setAutoCancel(true);
                        notificationManager.notify(1, notificationBuilder.build());
                    }
                    break;
                }
            }
        }
    }

    private void setupMatchProfileIntent(final String title, final String body, String uid, final String channel_id, final CharSequence channel_name, final int icon) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = Data.getInstance().getUrl() + "/getMatch";
        Map<String, String> params = new HashMap<>();
        params.put("uid", uid);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("Profile");
                            JSONObject object = array.getJSONObject(0);
                            String uid = object.getString("uid");
                            String name = object.getString("name");
                            String about = object.getString("about");
                            String interests = object.getString("interests");
                            String picture = object.getString("picture");
                            int timesCrossed = object.getInt("timesCrossed");
                            boolean approved = object.getBoolean("approved");

                            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            if (notificationManager != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                                    intent.putExtra("uid", uid);
                                    intent.putExtra("name", name);
                                    intent.putExtra("about", about);
                                    intent.putExtra("interests", interests);
                                    intent.putExtra("picture", picture);
                                    intent.putExtra("timesCrossed", String.valueOf(timesCrossed));
                                    intent.putExtra("approved", approved);
                                } else {
                                    intent = new Intent(getApplicationContext(), LoginActivity.class);
                                }
                                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                                        .setContentTitle(title)
                                        .setContentText(body)
                                        .setSmallIcon(icon)
                                        .setAutoCancel(true)
                                        .setContentIntent(pendingIntent);
                                notificationManager.notify(1, notificationBuilder.build());
                            }
                        } catch (JSONException j) {
                            j.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }
}