package com.HHMS;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NOTIFICATION_POLICY;
import static android.Manifest.permission.BODY_SENSORS;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
        ActivityCompat.requestPermissions((Activity) context, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BODY_SENSORS, ACCESS_NOTIFICATION_POLICY}, RequestPermissionCode);
    }

    public boolean CheckingPermissionIsEnabledOrNot() {

        return (ContextCompat.checkSelfPermission(context, BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

    }


}
