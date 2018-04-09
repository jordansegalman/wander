package me.vvander.wander;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class ChatViewHolder extends RecyclerView.ViewHolder {
    TextView messageText;

    ChatViewHolder(View itemView) {
        super(itemView);
        messageText = itemView.findViewById(R.id.messageText);
    }

    void bind(Message message) {
        messageText.setText(message.getMessage());
    }
}