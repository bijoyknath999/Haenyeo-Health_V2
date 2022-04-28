package com.rockwonitglobal.jejudiver;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NOTIFICATION_POLICY;
import static android.Manifest.permission.BODY_SENSORS;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_SMS;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.Manifest.permission.SEND_SMS;

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
        ActivityCompat.requestPermissions((Activity) context, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION,CALL_PHONE,RECEIVE_SMS
                ,SEND_SMS,READ_SMS ,ACCESS_NOTIFICATION_POLICY}, RequestPermissionCode);
    }

    public boolean CheckingPermissionIsEnabledOrNot() {

        return (ContextCompat.checkSelfPermission(context, ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, READ_SMS) == PackageManager.PERMISSION_GRANTED);

    }


}
