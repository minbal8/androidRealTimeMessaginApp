package com.ktuedu.rtMessaging.Models;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class localMessage implements Comparable<localMessage> {

    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);

    private String text;
    private String fromId;
    private String toId;
    private Date date;

    public localMessage() {
    }

    public localMessage(String text, String fromId, String toId) {
        this.text = text;
        this.fromId = fromId;
        this.toId = toId;
        date = new Date();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(String date) {
        try {
            setDate(formatter.parse(date));
        } catch (ParseException e) {
            Log.e("DateParse", "Error while parsing date");
        }

    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public int compareTo(localMessage localMessage) {
        return date.compareTo(localMessage.date);
    }
}
