package com.divadvo.babbleboosternew.data.local;

public class Session {

    long sessionStart;
    long sessionLength;

    public Session(long sessionStart, long sessionLength) {
        this.sessionStart = sessionStart;
        this.sessionLength = sessionLength;
    }

    public RealmSession generateRealmSession() {
        return new RealmSession(sessionStart, sessionLength);
    }

    @Override
    public boolean equals(Object obj) {
        Session other = (Session) obj;
        return (this.sessionStart == other.sessionStart) && (this.sessionLength == other.sessionLength);
    }
}
