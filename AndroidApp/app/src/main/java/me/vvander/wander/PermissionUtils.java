package me.vvander.wander;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

public abstract class PermissionUtils {
    public static void requestPermission(AppCompatActivity activity, int requestId, String permission, boolean finishActivity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            // Display a dialog with rationale.
            //PermissionUtils.RationaleDialog.newInstance(requestId, finishActivity)
            //        .show(activity.getSupportFragmentManager(), "dialog");
        } else {
            // Location permission has not been granted yet, request it.
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestId);
        }
    }
}