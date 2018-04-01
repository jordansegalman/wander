package me.vvander.wander;

import android.content.Context;

import com.google.firebase.iid.FirebaseInstanceId;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;

public class Data {
    private static final String url = "https://vvander.me";
    private static final Data data = new Data();
    private boolean loggedIn = false;
    private boolean loggedInGoogle = false;
    private boolean loggedInFacebook = false;
    private PersistentCookieStore persistentCookieStore;
    private String firebaseRegistrationToken;
    private boolean manualLocationSwitch = true;
    private boolean scheduleLocationSwitch = true;
    private boolean activityRecognitionLocationSwitch = false;
    private boolean disableNotifications = false;
    private ArrayList<LocationScheduleItem> locationSchedule;

    static Data getInstance() {
        return data;
    }

    String getUrl() {
        return url;
    }

    void login() {
        loggedIn = true;
    }

    void logout() {
        loggedIn = false;
    }

    boolean getLoggedIn() {
        return loggedIn;
    }

    void loginGoogle() {
        loggedInGoogle = true;
    }

    void logoutGoogle() {
        loggedInGoogle = false;
    }

    boolean getLoggedInGoogle() {
        return loggedInGoogle;
    }

    void loginFacebook() {
        loggedInFacebook = true;
    }

    void logoutFacebook() {
        loggedInFacebook = false;
    }

    boolean getLoggedInFacebook() {
        return loggedInFacebook;
    }

    void initializeCookies(Context context) {
        persistentCookieStore = new PersistentCookieStore(context);
        CookieHandler.setDefault(new CookieManager(persistentCookieStore, CookiePolicy.ACCEPT_ALL));
    }

    void removeAllCookies() {
        persistentCookieStore.removeAll();
    }

    void initializeFirebaseRegistrationToken() {
        String token = FirebaseInstanceId.getInstance().getToken();
        if (token != null) {
            setFirebaseRegistrationToken(token);
        }
    }

    String getFirebaseRegistrationToken() {
        return firebaseRegistrationToken;
    }

    void setFirebaseRegistrationToken(String token) {
        firebaseRegistrationToken = token;
    }

    boolean getManualLocationSwitch() {
        return manualLocationSwitch;
    }

    void setManualLocationSwitch(boolean value) {
        manualLocationSwitch = value;
    }

    boolean getScheduleLocationSwitch() {
        return scheduleLocationSwitch;
    }

    void setScheduleLocationSwitch(boolean value) {
        scheduleLocationSwitch = value;
    }

    boolean getActivityRecognitionLocationSwitch() {
        return activityRecognitionLocationSwitch;
    }

    void setActivityRecognitionLocationSwitch(boolean value) {
        activityRecognitionLocationSwitch = value;
    }

    boolean getDisableNotifications(){
        return disableNotifications;
    }
    void setDisableNotifications(boolean value){
        disableNotifications = value;
    }

    void initializeLocationSchedule() {
        locationSchedule = new ArrayList<>();
    }

    ArrayList<LocationScheduleItem> getLocationSchedule() {
        return locationSchedule;
    }

    void setLocationSchedule(ArrayList<LocationScheduleItem> locationSchedule) {
        this.locationSchedule = locationSchedule;
    }
}