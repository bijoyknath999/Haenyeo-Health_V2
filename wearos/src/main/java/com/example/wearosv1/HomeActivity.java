package com.example.wearosv1;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
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
import android.support.wearable.input.WearableButtons;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class HomeActivity extends Activity implements
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener {

    private Button button;
    private TextView mTextView;

    private GoogleApiClient googleClient;
    TextView TextHearRate;
    ImageView ImgHeart;
    private ObjectAnimator animator;
    private SensorManager sensorService;
    private Sensor heartSensor;
    private LinearLayout HeartRateClick;
    public static int i = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        /*button = findViewById(R.id.get_location);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GpsTracker gpsTracker = new GpsTracker(MainActivity.this);
                Log.d("Loc",""+gpsTracker.getLatitude());
            }
        });*/

        //set up google play services client
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        TextHearRate = findViewById(R.id.text_heart_rate);
        ImgHeart = findViewById(R.id.image_heart);
        HeartRateClick = findViewById(R.id.home_heart_click);


        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                != PackageManager.PERMISSION_GRANTED) {

            String[] permissions = new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            requestPermissions(permissions, 1);

        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            requestPermissions(permissions, 1);

        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
            requestPermissions(permissions, 1);

        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            String[] permissions = new String[]{Manifest.permission.BODY_SENSORS};
            requestPermissions(permissions, 1);

        } else {
            getHeartRate();
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            //mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 100, 0);
        }

        RequestChecker requestChecker = new RequestChecker();
        HeartRateClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestChecker.CheckingPermissionIsEnabledOrNot(HomeActivity.this))
                    getHeartRate();
                else
                    requestChecker.RequestMultiplePermission();
            }
        });

    }


    void SendSosData()
    {
        GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);
        double lat = gpsTracker.getLatitude();
        double lon = gpsTracker.getLongitude();
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/Haenyeo_Health");
        putDataMapReq.getDataMap().putDouble("lat", lat);
        putDataMapReq.getDataMap().putDouble("lon", lon);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi
                        .putDataItem(googleClient, putDataReq);
    }

    void sendData(String heartrate) {


        GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getHeartRate();
        } else {
            Toast.makeText(getApplicationContext(), "Permission declined. App will not function", Toast.LENGTH_LONG).show();
        }
    }

    void getHeartRate() {
        Toast.makeText(getApplicationContext(), "Checking......", Toast.LENGTH_LONG).show();
        sensorService = (SensorManager) getSystemService(SENSOR_SERVICE);
        heartSensor = sensorService.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorService.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);

        animator = ObjectAnimator.ofPropertyValuesHolder(ImgHeart, PropertyValuesHolder.ofFloat("scaleX", 0.8F),
                PropertyValuesHolder.ofFloat("scaleY", 0.8f));
        animator.setDuration(300);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            int heart_rate = (int) event.values[0];
            if (heart_rate > 0) {
                animator.setDuration(60000 / heart_rate);
                animator.start();
            }
            //Toast.makeText(getApplicationContext(), Float.toString(heart_rate), Toast.LENGTH_LONG).show();
            TextHearRate.setText(Integer.toString(heart_rate) +"");
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

    //on resuming activity, reconnect play services
    public void onResume(){
        super.onResume();
        googleClient.connect();
    }

    //on suspended connection, remove play services
    public void onConnectionSuspended(int cause) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

    //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        Intent intent = new Intent( this, BgService.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            startForegroundService(intent);
        }
        else
        {
            startService(intent);
        }
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
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

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
            i++;
            if(i==2){
                SendSosData();
                i=0;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
}