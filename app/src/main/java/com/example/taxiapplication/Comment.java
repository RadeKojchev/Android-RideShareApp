package com.example.taxiapplication;

public class Comment {
    private String passengerName;
    private String text;
    private String timestamp;

    public Comment(String passengerName, String text, String timestamp) {
        this.passengerName = passengerName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
