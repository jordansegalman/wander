package me.vvander.wander;

import android.content.Context;

import com.google.firebase.iid.FirebaseInstanceId;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class Data {
    private static final String url = "https://vvander.me";
    private boolean loggedIn = false;
    private boolean loggedInGoogle = false;
    private boolean loggedInFacebook = false;
    private PersistentCookieStore persistentCookieStore;
    private String firebaseRegistrationToken;
    private boolean valid = true;

    public void login() {
        loggedIn = true;
    }

    public void logout() {
        loggedIn = false;
    }

    public boolean getLoggedIn() {
        return loggedIn;
    }

    public void loginGoogle() {
        loggedInGoogle = true;
    }

    public void logoutGoogle() {
        loggedInGoogle = false;
    }

    public boolean getLoggedInGoogle() {
        return loggedInGoogle;
    }

    public void loginFacebook() {
        loggedInFacebook = true;
    }

    public void logoutFacebook() {
        loggedInFacebook = false;
    }

    public boolean getLoggedInFacebook() {
        return loggedInFacebook;
    }

    public void initializeCookies(Context context) {
        persistentCookieStore = new PersistentCookieStore(context);
        CookieHandler.setDefault(new CookieManager(persistentCookieStore, CookiePolicy.ACCEPT_ALL));
    }

    public void removeAllCookies() {
        persistentCookieStore.removeAll();
    }

    public String getUrl() {
        return url;
    }

    public void initializeFirebaseRegistrationToken() {
        String token = FirebaseInstanceId.getInstance().getToken();
        if (token != null) {
            setFirebaseRegistrationToken(token);
        }
    }

    public void setFirebaseRegistrationToken(String token) {
        firebaseRegistrationToken = token;
    }

    public String getFirebaseRegistrationToken() {
        return firebaseRegistrationToken;
    }

    public void setValidity(boolean valid) {
        this.valid = valid;
    }

    public boolean getValidity() {
        return valid;
    }

    private static final Data data = new Data();

    public static Data getInstance() {
        return data;
    }
}