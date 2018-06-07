package com.divadvo.babbleboosternew.data.local;


import io.realm.RealmObject;

public class RealmSession extends RealmObject {

    long sessionStart;
    long sessionLength;

    public RealmSession(long sessionStart, long sessionLength) {
        this.sessionStart = sessionStart;
        this.sessionLength = sessionLength;
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
        return new Session(sessionStart, sessionLength);
    }
}
