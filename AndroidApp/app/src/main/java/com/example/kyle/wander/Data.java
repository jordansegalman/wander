package com.example.kyle.wander;

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
    private String url;
    private String username;

    public String getUsername(){return username;}

    public String getUrl() {
        return url;
    }

    public void setUsername(String username){this.username = username;}

    public void setUrl(String url) {
        this.url = url;
    }

    private static final Data data = new Data();

    public static Data getInstance() {
        return data;
    }
}
