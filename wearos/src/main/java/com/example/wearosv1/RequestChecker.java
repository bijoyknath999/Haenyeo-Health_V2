package com.example.wearosv1;

import static android.Manifest.permission.ACCESS_NOTIFICATION_POLICY;
import static android.Manifest.permission.BODY_SENSORS;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class RequestChecker extends Activity {

    public static final int RequestPermissionCode = 7;

    public void RequestMultiplePermission() {

        // Creating String Array with Permissions.
        ActivityCompat.requestPermissions(RequestChecker.this, new String[]
                {BODY_SENSORS,ACCESS_NOTIFICATION_POLICY
                }, RequestPermissionCode);

    }
    // Calling override method.
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case RequestPermissionCode:

                if (grantResults.length > 0) {

                    boolean SensorPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean NotificationPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (SensorPermission && NotificationPermission) {

                        Toast.makeText(RequestChecker.this, "Permission Granted", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(RequestChecker.this,"Permission Denied",Toast.LENGTH_LONG).show();

                    }
                }

                break;
        }
    }

    public static boolean CheckingPermissionIsEnabledOrNot(Context context) {

        int FirstPermissionResult = ActivityCompat.checkSelfPermission(context, BODY_SENSORS);
        int SecondPermissionResult = ActivityCompat.checkSelfPermission(context, ACCESS_NOTIFICATION_POLICY);

        return FirstPermissionResult == PackageManager.PERMISSION_GRANTED &&
                SecondPermissionResult == PackageManager.PERMISSION_GRANTED;
    }
}
