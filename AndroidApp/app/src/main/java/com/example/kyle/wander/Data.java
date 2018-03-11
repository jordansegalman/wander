package com.example.kyle.wander;

import android.content.Context;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * This is a singleton class that will store data that we need shared between
 * multiple classes. This includes information like URL, username, email, etc.
 *
 * If you guys believe an application singleton would be better, feel free to modify.
 *
 * https://stackoverflow.com/questions/4878159/whats-the-best-way-to-share-data-between-activities
 * https://www.geeksforgeeks.org/singleton-class-java/
 */
public class Data {
    private PersistentCookieStore persistentCookieStore;

    public void initializeCookies(Context context) {
        persistentCookieStore = new PersistentCookieStore(context);
        CookieHandler.setDefault(new CookieManager(persistentCookieStore, CookiePolicy.ACCEPT_ALL));
    }

    public void removeCookies() {
        persistentCookieStore.removeAll();
    }

    public String getUrl() {
        return "https://vvander.me";
    }

    private static final Data data = new Data();

    public static Data getInstance() {
        return data;
    }
}