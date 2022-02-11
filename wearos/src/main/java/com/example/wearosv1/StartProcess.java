package com.example.wearosv1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Toast;

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

public class StartProcess extends Activity implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener {

    public static GoogleApiClient googleClient;
    private SensorManager sensorService;
    private Sensor heartSensor;

    public static void StartProcess (Context context)
    {
        /*//set up google play services client
        googleClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) context)
                .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) context)
                .build();*/


        RequestChecker requestChecker = new RequestChecker();
        if(requestChecker.CheckingPermissionIsEnabledOrNot(requestChecker.getApplicationContext()))
        {
            /*getHeartRate();
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            //mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 100, 0);*/
        }
        else {
            requestChecker.RequestMultiplePermission();
        }
    }


    //on resuming activity, reconnect play services
    public void onResume(){
        super.onResume();
        googleClient.connect();
    }
    //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }


    void sendData(String heartrate) {


        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/wearosheart");
        putDataMapReq.getDataMap().putString("HeartRate", heartrate);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi
                        .putDataItem(googleClient, putDataReq);


    }

    public void getHeartRate() {
        Toast.makeText(getApplicationContext(), "Loading......", Toast.LENGTH_LONG).show();
        sensorService = (SensorManager) getSystemService(SENSOR_SERVICE);
        heartSensor = sensorService.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorService.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopall()
    {
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
        sensorService.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            int heart_rate = (int) event.values[0];
            sendData(Integer.toString(heart_rate));

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

    //on suspended connection, remove play services
    public void onConnectionSuspended(int cause) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

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
}
