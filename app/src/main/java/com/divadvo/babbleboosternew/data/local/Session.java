package com.divadvo.babbleboosternew.data.local;

public class Session {

    long sessionStart;
    long sessionLength;
    String screenName;

    public Session(long sessionStart, long sessionLength, String screenName) {
        this.sessionStart = sessionStart;
        this.sessionLength = sessionLength;
        this.screenName = screenName;
    }

    public Session() {

    }

    public RealmSession generateRealmSession() {
        return new RealmSession(sessionStart, sessionLength, screenName);
    }

    @Override
    public boolean equals(Object obj) {
        Session other = (Session) obj;
        return (this.sessionStart == other.sessionStart) && (this.sessionLength == other.sessionLength);
    }
}
