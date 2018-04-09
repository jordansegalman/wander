package me.vvander.wander;

enum MessageType {
    SENT, RECEIVED
}

public class Message {
    private String message;
    private long time;
    private MessageType messageType;

    Message(String message, long time, MessageType messageType) {
        this.message = message;
        this.time = time;
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }

    public MessageType getType() {
        return messageType;
    }
}