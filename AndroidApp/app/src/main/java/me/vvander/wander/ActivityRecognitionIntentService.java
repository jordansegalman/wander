package me.vvander.wander;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionIntentService extends IntentService {
    private static final String TAG = ActivityRecognitionIntentService.class.getSimpleName();

    public ActivityRecognitionIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity detectedActivity = result.getMostProbableActivity();
            switch (detectedActivity.getType()) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.d(TAG, "IN VEHICLE - CONFIDENCE: " + detectedActivity.getConfidence());
                    Data.getInstance().setActivityRecognitionLocationSwitch(false);
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    Log.d(TAG, "ON BICYCLE - CONFIDENCE: " + detectedActivity.getConfidence());
                    Data.getInstance().setActivityRecognitionLocationSwitch(true);
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.d(TAG, "ON FOOT - CONFIDENCE: " + detectedActivity.getConfidence());
                    Data.getInstance().setActivityRecognitionLocationSwitch(true);
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.d(TAG, "RUNNING - CONFIDENCE: " + detectedActivity.getConfidence());
                    Data.getInstance().setActivityRecognitionLocationSwitch(true);
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.d(TAG, "STILL - CONFIDENCE: " + detectedActivity.getConfidence());
                    Data.getInstance().setActivityRecognitionLocationSwitch(false);
                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.d(TAG, "TILTING - CONFIDENCE: " + detectedActivity.getConfidence());
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    Log.d(TAG, "UNKNOWN - CONFIDENCE: " + detectedActivity.getConfidence());
                    break;
                }
                case DetectedActivity.WALKING: {
                    Log.d(TAG, "WALKING - CONFIDENCE: " + detectedActivity.getConfidence());
                    Data.getInstance().setActivityRecognitionLocationSwitch(true);
                    break;
                }
            }
        }
    }
}