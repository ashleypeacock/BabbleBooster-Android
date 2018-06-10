package com.divadvo.babbleboosternew.data.local;


import io.realm.RealmObject;

public class RealmSession extends RealmObject {

    long sessionStart;
    long sessionLength;
    String className;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    String username;

    public RealmSession(long sessionStart, long sessionLength, String className, String username) {
        this.sessionStart = sessionStart;
        this.sessionLength = sessionLength;
        this.className = className;
        this.username = username;
    }

    public RealmSession() {

    }

    public long getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(long sessionStart) {
        this.sessionStart = sessionStart;
    }

    public long getSessionLength() {
        return sessionLength;
    }

    public void setSessionLength(long sessionLength) {
        this.sessionLength = sessionLength;
    }

    public Session generateSession() {
        return new Session(sessionStart, sessionLength, className, username);
    }
}
