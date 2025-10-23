package com.example.doorlock;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class LogEntry {
    public String uid;
    public String action;
    @ServerTimestamp
    public Date timestamp;

    public LogEntry() {} // Required empty constructor

    public LogEntry(String uid, String action) {
        this.uid = uid;
        this.action = action;
    }
}
