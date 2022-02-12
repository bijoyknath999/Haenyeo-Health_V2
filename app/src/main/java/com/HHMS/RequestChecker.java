package com.HHMS;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NOTIFICATION_POLICY;
import static android.Manifest.permission.BODY_SENSORS;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class RequestChecker extends Activity {

    public static final int RequestPermissionCode = 7;

    private final Context context;

    public RequestChecker(Context context)
    {
        this.context = context;
    }

    public void RequestMultiplePermission() {
        ActivityCompat.requestPermissions((Activity) context, new String[]{ACCESS_FINE_LOCATION}, RequestPermissionCode);
    }

    public boolean CheckingPermissionIsEnabledOrNot() {

        Log.d("req","testing");

        return ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

    }


}
