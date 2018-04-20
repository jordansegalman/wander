package me.vvander.wander;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = ChatActivity.class.getSimpleName();
    private static final int MAX_MESSAGE_LENGTH = 1024;
    private RequestQueue requestQueue;
    private String uid;
    private String matchUid;
    private Socket socket;
    private boolean initialized = false;
    private ArrayList<Message> messages;
    private ChatAdapter chatAdapter;
    private LinearLayoutManager linearLayoutManager;
    private ImageButton sendButton;
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Socket.IO connected");
                    if (!initialized) {
                        initialize();
                    }
                }
            });
        }
    };
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Socket.IO connection error");
                }
            });
        }
    };
    private Emitter.Listener onConnectTimeout = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Socket.IO connection timeout");
                }
            });
        }
    };
    private Emitter.Listener onMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = new JSONObject((String) args[0]);
                        String from = data.getString("from");
                        String message = data.getString("message");
                        long time = data.getLong("time");
                        String name = data.getString("name");
                        if (from.equals(matchUid)) {
                            messages.add(new Message(message, time, MessageType.RECEIVED));
                            if (messages.size() > 1 && messages.get(messages.size() - 1).getTime() < messages.get(messages.size() - 2).getTime()) {
                                Collections.sort(messages, new Comparator<Message>() {
                                    @Override
                                    public int compare(Message o1, Message o2) {
                                        return Long.compare(o1.getTime(), o2.getTime());
                                    }
                                });
                                chatAdapter.notifyDataSetChanged();
                            } else {
                                chatAdapter.notifyItemInserted(messages.size() - 1);
                            }
                            linearLayoutManager.scrollToPosition(messages.size() - 1);
                            Log.d(TAG, "Socket.IO message received");
                        } else {
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
                                    intent.putExtra("UID", from);
                                    intent.putExtra("name", name);
                                } else {
                                    intent = new Intent(getApplicationContext(), LoginActivity.class);
                                }
                                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                                        .setContentTitle(name)
                                        .setContentText(message)
                                        .setSmallIcon(R.drawable.message_notification_icon)
                                        .setAutoCancel(true)
                                        .setContentIntent(pendingIntent);
                                notificationManager.notify(1, notificationBuilder.build());
                            }
                            Log.d(TAG, "Socket.IO message notification sent");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        uid = Data.getInstance().getUid();
        matchUid = getIntent().getStringExtra("UID");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getIntent().getStringExtra("name"));
        }

        requestQueue = Volley.newRequestQueue(this);

        sendButton = findViewById(R.id.messageSendButton);
        sendButton.setEnabled(false);

        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        linearLayoutManager = new LinearLayoutManager(this);
        getMessages();

        RecyclerView messagesRecyclerView = findViewById(R.id.messages);
        messagesRecyclerView.setLayoutManager(linearLayoutManager);
        messagesRecyclerView.setAdapter(chatAdapter);

        connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (socket.connected() && !initialized) {
            messages.clear();
            getMessages();
            initialize();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendButton.setEnabled(false);
        if (socket.connected() && initialized) {
            terminate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket.connected()) {
            if (initialized) {
                terminate();
            }
            disconnect();
        }
    }

    private void getMessages() {
        String url = Data.getInstance().getUrl() + "/getMessages";
        Map<String, String> params = new HashMap<>();
        params.put("uid", matchUid);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("Messages");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                String uidFrom = object.getString("uidFrom");
                                String uidTo = object.getString("uidTo");
                                String message = object.getString("message");
                                long time = object.getLong("time");
                                if (uidFrom.equals(Data.getInstance().getUid()) && uidTo.equals(matchUid)) {
                                    messages.add(new Message(message, time, MessageType.SENT));
                                } else if (uidFrom.equals(matchUid) && uidTo.equals(Data.getInstance().getUid())) {
                                    messages.add(new Message(message, time, MessageType.RECEIVED));
                                }
                            }
                            Collections.sort(messages, new Comparator<Message>() {
                                @Override
                                public int compare(Message o1, Message o2) {
                                    return Long.compare(o1.getTime(), o2.getTime());
                                }
                            });
                            chatAdapter.notifyDataSetChanged();
                            linearLayoutManager.scrollToPosition(messages.size() - 1);
                            sendButton.setEnabled(true);
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

    private void connect() {
        try {
            socket = IO.socket(Data.getInstance().getUrl());
            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectTimeout);
            socket.on("message", onMessage);
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void initialize() {
        try {
            JSONObject data = new JSONObject();
            data.put("uid", uid);
            socket.emit("initialize", data.toString());
            initialized = true;
            Log.d(TAG, "Socket.IO initialized");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void terminate() {
        try {
            JSONObject data = new JSONObject();
            data.put("uid", uid);
            socket.emit("terminate", data.toString());
            initialized = false;
            Log.d(TAG, "Socket.IO terminated");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        socket.disconnect();
        socket.off();
        Log.d(TAG, "Socket.IO disconnected");
    }

    public void send(View view) {
        if (socket.connected() && initialized) {
            EditText messageText = findViewById(R.id.messageEditText);
            String message = messageText.getText().toString();
            if (!TextUtils.isEmpty(message) && message.length() <= MAX_MESSAGE_LENGTH) {
                sendMessage(message);
                messageText.getText().clear();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Not connected!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String message) {
        try {
            long time = System.currentTimeMillis();
            JSONObject data = new JSONObject();
            data.put("to", matchUid);
            data.put("from", uid);
            data.put("message", message);
            data.put("time", time);
            socket.emit("message", data.toString());
            messages.add(new Message(message, time, MessageType.SENT));
            if (messages.size() > 1 && messages.get(messages.size() - 1).getTime() < messages.get(messages.size() - 2).getTime()) {
                Collections.sort(messages, new Comparator<Message>() {
                    @Override
                    public int compare(Message o1, Message o2) {
                        return Long.compare(o1.getTime(), o2.getTime());
                    }
                });
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemInserted(messages.size() - 1);
            }
            linearLayoutManager.scrollToPosition(messages.size() - 1);
            Log.d(TAG, "Socket.IO message sent");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}