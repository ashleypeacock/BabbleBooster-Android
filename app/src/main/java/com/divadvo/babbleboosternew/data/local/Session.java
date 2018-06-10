package com.divadvo.babbleboosternew.data.local;

public class Session {

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

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public long sessionStart;
    public long sessionLength;
    public String screenName;
    public String username;

    public Session(long sessionStart, long sessionLength, String screenName, String username) {
        this.sessionStart = sessionStart;
        this.sessionLength = sessionLength;
        this.screenName = screenName;
        this.username = username;
    }

    public Session() {

    }

    public RealmSession generateRealmSession() {
        return new RealmSession(sessionStart, sessionLength, screenName, username);
    }

    @Override
    public boolean equals(Object obj) {
        Session other = (Session) obj;
        return (this.sessionStart == other.sessionStart) && (this.sessionLength == other.sessionLength) && (this.username.equals(other.username));
    }
}
