package com.example.wearosv1;

import static java.security.AccessController.getContext;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
        GoogleApiClient.OnConnectionFailedListener,SensorEventListener{


    private SensorManager sensorService;
    private Sensor heartSensor;
    public static GoogleApiClient googleClient;


    // execution of service will start
    // on calling this method
    public int onStartCommand(Intent intent, int flags, int startId) {

        googleClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        createNotificationChannel();
        Intent intent1 = new Intent ( this, MainActivity.class);
        PendingIntent pendingIntent_ = PendingIntent.getActivity( this, 0,intent1, 0);
        Notification notification_ = new NotificationCompat.Builder( this, "ChannelIdl")
                .setContentTitle("My App tutorial")
                .setContentText("Our application is")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent_).build();
        startForeground( 1, notification_);


        Log.d("Bg","Bging");
        RequestChecker requestChecker = new RequestChecker();
        if(requestChecker.CheckingPermissionIsEnabledOrNot(getApplicationContext()))
        {
            getHeartRate();
        }
        else {
            requestChecker.RequestMultiplePermission();
        }

        googleClient.connect();
        return START_STICKY;
    }


    private void createNotificationChannel()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel("ChannelId1",
                    "Foreground notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    // execution of the service will
    // stop on calling this method
    public void onDestroy() {
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
        sensorService.unregisterListener(this);
        stopForeground(true);
        stopSelf();
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
}