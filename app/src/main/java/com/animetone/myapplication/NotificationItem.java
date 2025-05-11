package com.animetone.myapplication;

import java.util.Objects;

public class NotificationItem {
    private String sender;
    private String message;
    private String timestamp;

    public NotificationItem(String sender, String message, String timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationItem)) return false;
        NotificationItem that = (NotificationItem) o;
        return sender.equals(that.sender) &&
                message.equals(that.message) &&
                timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, message, timestamp);
    }
}

