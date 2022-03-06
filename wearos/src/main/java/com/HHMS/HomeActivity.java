package com.HHMS;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class HomeActivity extends Activity
        implements SensorEventListener, DataClient.OnDataChangedListener {

    private Button button;
    private TextView mTextView;
    TextView TextHearRate;
    ImageView ImgHeart;
    private ObjectAnimator animator;
    private SensorManager sensorService;
    private Sensor heartSensor;
    private LinearLayout HeartRateClick, LocationClick;
    public static int i = 0;
    private LinearLayout SOS;


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


        TextHearRate = findViewById(R.id.text_heart_rate);
        ImgHeart = findViewById(R.id.image_heart);
        HeartRateClick = findViewById(R.id.home_heart_click);
        SOS = findViewById(R.id.home_sos);
        LocationClick = findViewById(R.id.home_check_location);

        RequestChecker requestChecker = new RequestChecker(HomeActivity.this);
        if (requestChecker.CheckingPermissionIsEnabledOrNot())
            getHeartRate();
        else
            requestChecker.RequestMultiplePermission();

        HeartRateClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestChecker.CheckingPermissionIsEnabledOrNot())
                    getHeartRate();
                else
                    requestChecker.RequestMultiplePermission();
            }
        });


        SOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendSosData(true);
            }
        });

        LocationClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, MapsActivity.class));
            }
        });


    }


    void SendSosData(boolean sos)
    {
        DataClient dataclient = Wearable.getDataClient(getApplicationContext());
        GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);
        double lat = gpsTracker.getLatitude();
        double lon = gpsTracker.getLongitude();
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/Haenyeo_Health");
        putDataMapReq.getDataMap().putString("HeartRate", "0");
        putDataMapReq.getDataMap().putDouble("lat", lat);
        putDataMapReq.getDataMap().putDouble("lon", lon);
        putDataMapReq.getDataMap().putBoolean("sos", sos);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = dataclient.putDataItem(putDataReq);

    }

    void sendData(String heartrate) {

        DataClient dataclient = Wearable.getDataClient(getApplicationContext());

        GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);
        double lat = gpsTracker.getLatitude();
        double lon = gpsTracker.getLongitude();
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/Haenyeo_Health");
        putDataMapReq.getDataMap().putString("HeartRate", heartrate);
        putDataMapReq.getDataMap().putDouble("lat", lat);
        putDataMapReq.getDataMap().putDouble("lon", lon);
        putDataMapReq.getDataMap().putBoolean("sos", false);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = dataclient.putDataItem(putDataReq);

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
            if (heart_rate>=40 && heart_rate<=220)
            {
                animator.setDuration(60000 / heart_rate);
                animator.start();
                TextHearRate.setText(Integer.toString(heart_rate) +"");
                sendData(Integer.toString(heart_rate));
            }
        }
        else {
            // Do nothing
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public boolean foregroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if(BgService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    //on resuming activity, reconnect play services
    public void onResume(){
        super.onResume();
        Wearable.getDataClient(this).addListener(this);

        Intent startIntent = new Intent(HomeActivity.this, BgService.class);
        startIntent.setAction("stop");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            if (foregroundServiceRunning())
                startForegroundService(startIntent);
        }
        else
        {
            if (foregroundServiceRunning())
                startService(startIntent);
        }
    }

    //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);

        Intent startIntent = new Intent(HomeActivity.this, BgService.class);
        startIntent.setAction("start");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            if (!foregroundServiceRunning())
                startForegroundService(startIntent);
        }
        else
        {
            if (!foregroundServiceRunning())
                startService(startIntent);
        }
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
                Toast.makeText(HomeActivity.this,"SOS Data Send Successfully !!", Toast.LENGTH_SHORT).show();
                i=0;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
}