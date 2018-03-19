package com.example.kyle.wander;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * Created by Brandon on 3/18/2018.
 */

public class ActivityRecognizedService extends IntentService{

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities( result.getProbableActivities() );
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
    for( DetectedActivity activity : probableActivities){
        switch(activity.getType()){
            case DetectedActivity.IN_VEHICLE: {
            Log.e("ActivityRecognition","IN VEHICLE");
            Data.getInstance().setValidity(FALSE);
            break;
            }
            case DetectedActivity.ON_BICYCLE: {
                Log.e("ActivityRecognition","ON BIKE");
                Data.getInstance().setValidity(TRUE);
                break;
            }
            case DetectedActivity.ON_FOOT: {
                Log.e("ActivityRecognition", "ON FOOT");
                Data.getInstance().setValidity(TRUE);
                break;
            }
            case DetectedActivity.RUNNING: {
                Log.e("ActivityRecognition","USER RUNNING");
                Data.getInstance().setValidity(TRUE);
                break;
            }
            case DetectedActivity.STILL: {
                Log.e("ActivityRecognition", "NOT MOVING");
                Data.getInstance().setValidity(FALSE);
                break;
            }
            case DetectedActivity.TILTING: {
                Log.e("ActivityRecognition", "TILTED");
                break;
            }
            case DetectedActivity.UNKNOWN: {
                Log.e("ActivityRecognition", "NOT SURE");
                break;
            }
            case DetectedActivity.WALKING: {
                Log.e("ActivityRecognition", "WALKING");
                Data.getInstance().setValidity(TRUE);
                break;
            }
        }
    }
    }
}
