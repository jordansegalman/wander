package com.example.kyle.wander;

import java.io.File;

/**
 * This is a singleton class that will store data that we need shared between
 * multiple classes. This includes information like URL, session ID, etc.
 *
 * If you guys believe an application singleton would be better, feel free to modify.
 *
 * https://stackoverflow.com/questions/4878159/whats-the-best-way-to-share-data-between-activities
 * https://www.geeksforgeeks.org/singleton-class-java/
 */
public class Data {
    private final String url = "https://vvander.me";
    private final String sessionInfoFile = "sessionInfoFile";
    private String username;
    private String sessionId;
    private String email;
    private boolean valid = true;

    public String getUsername(){return username;}

    public String getSessionId() {return sessionId;}

    public String getSessionInfoFile() {
        return sessionInfoFile;
    }

    public String getUrl() {
        return url;
    }

    public String getEmail() {return email;}

    public boolean getValidity(){return valid;}

    public void setValidity(boolean valid){this.valid = valid;}

    public void setUsername(String username){this.username = username;}

    public void setEmail(String email){this.email = email;}

    public void setSessionId(String sessionId) {this.sessionId = sessionId;}

    private static final Data data = new Data();

    public static Data getInstance() {
        return data;
    }
}
