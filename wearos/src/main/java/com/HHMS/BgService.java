package com.HHMS;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NOTIFICATION_POLICY;
import static android.Manifest.permission.BODY_SENSORS;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class BgService extends Service implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,SensorEventListener, KeyListener {


    private SensorManager sensorService;
    private Sensor heartSensor;
    public static GoogleApiClient googleClient;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";


    // execution of service will start
    // on calling this method
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals("start")) {
            googleClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            createNotificationChannel();
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("해녀건강")
                    .setContentText("Health Service is running")
                    .setSmallIcon(R.drawable.ic_notifications)
                    .build();
            startForeground(1, notification);
            //do heavy work on a background thread
            //stopSelf();
            Log.d("Bg","Bging");
            RequestChecker requestChecker = new RequestChecker(getApplicationContext());
            if (requestChecker.CheckingPermissionIsEnabledOrNot())
            {
                getHeartRate();
            }

            googleClient.connect();
        }
        else if (intent.getAction().equals("stop")) {
            stopall();
        }



        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "해녀건강 Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public  void stopall()
    {
        if (googleClient!=null)
        {
            Wearable.DataApi.removeListener(googleClient, this);
            googleClient.disconnect();
            stopForeground(true);
            stopSelf();
        }
        if (sensorService!=null)
        {
            sensorService.unregisterListener(this);
        }
    }

    @Override
    // execution of the service will
    // stop on calling this method
    public void onDestroy() {
        stopall();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void getHeartRate() {
        Toast.makeText(getApplicationContext(), "Loading......", Toast.LENGTH_LONG).show();
        sensorService = (SensorManager) getSystemService(SENSOR_SERVICE);
        heartSensor = sensorService.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorService.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            int heart_rate = (int) event.values[0];
            sendData(Integer.toString(heart_rate));

            Log.d("data2","->rate : "+heart_rate);
        } else {
            // Do nothing
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    //on successful connection to play services, add data listner
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(googleClient, this);
    }

    /*//on resuming activity, reconnect play services
    public void onResume(){
        super.onResume();
        stopService(new Intent(HomeActivity.this,BgService.class));
        googleClient.connect();
    }*/

    //on suspended connection, remove play services
    public void onConnectionSuspended(int cause) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

   /* //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        startService(new Intent(HomeActivity.this,BgService.class));
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }*/

    //On failed connection to play services, remove the data listener
    public void onConnectionFailed(ConnectionResult result) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

    //function triggered every time there's a data change event
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event: dataEvents){

            //data item changed
            if(event.getType() == DataEvent.TYPE_CHANGED){

                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);

                //RESPONSE back from mobile message
                if(item.getUri().getPath().equals("/responsemessage")){

                }
            }
        }
    }

    void sendData(String heartrate) {


        GpsTracker gpsTracker = new GpsTracker(getApplicationContext());
        double lat = gpsTracker.getLatitude();
        double lon = gpsTracker.getLongitude();
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/Haenyeo_Health");
        putDataMapReq.getDataMap().putString("HeartRate", heartrate);
        putDataMapReq.getDataMap().putDouble("lat", lat);
        putDataMapReq.getDataMap().putDouble("lon", lon);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi
                        .putDataItem(googleClient, putDataReq);

    }

    @Override
    public int getInputType() {
        return 0;
    }

    @Override
    public boolean onKeyDown(View view, Editable editable, int i, KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_POWER) {
            i++;
            if(i==2){
                Toast.makeText(getApplicationContext(),"SOS Data Send Successfully !!", Toast.LENGTH_SHORT).show();
                i=0;
            }

        }
        return true;
    }

    @Override
    public boolean onKeyUp(View view, Editable editable, int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onKeyOther(View view, Editable editable, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void clearMetaKeyState(View view, Editable editable, int i) {

    }
}