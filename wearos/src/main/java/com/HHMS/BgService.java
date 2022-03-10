package com.HHMS;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

import com.HHMS.models.Result;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BgService extends Service implements SensorEventListener, DataClient.OnDataChangedListener {


    private SensorManager sensorService;
    private Sensor heartSensor;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    public int finalheartrate;
    private SharedPreferences sharedPreferences;
    private CountDownTimer cdt;


    // execution of service will start
    // on calling this method
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (intent!=null)
        {

            if (intent.getAction().equals("start")) {
                Wearable.getDataClient(this).addListener(this);

                createNotificationChannel();
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Woman Sea Diver")
                        .setContentText("Health Service is running")
                        .setSmallIcon(R.drawable.ic_notifications)
                        .build();
                startForeground(1, notification);
                //do heavy work on a background thread
                //stopSelf();
                Log.d("Bg","Bging");
                RunTimer();
            }
            else if (intent.getAction().equals("stop")) {
                stopall();
            }

        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Woman Sea Diver Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public  void stopall()
    {
        Wearable.getDataClient(this).removeListener(this);
        stopForeground(true);
        stopSelf();
        if (sensorService!=null)
        {
            sensorService.unregisterListener(this);
        }
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopall();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void getHeartRate() {
        Toast.makeText(getApplicationContext(), "Loading......", Toast.LENGTH_LONG).show();
        Log.d("Testing","Loading");
        sensorService = (SensorManager) getSystemService(SENSOR_SERVICE);
        heartSensor = sensorService.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorService.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void RunTimer() {
        sharedPreferences = getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        int time = sharedPreferences.getInt("time",0);

        if (time>0)
        {
            int finaltime = time*1000;
            cdt = new CountDownTimer(6000,1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    RequestChecker requestChecker = new RequestChecker(getApplicationContext());
                    if (requestChecker.CheckingPermissionIsEnabledOrNot())
                    {
                        getHeartRate();
                    }
                    SendHeartRateServer();
                    cdt.start();
                }
            };
            cdt.start();
        }
    }

    private void SendHeartRateServer()
    {


        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date();

        JSONObject data = new JSONObject();

        JSONObject jobj = new JSONObject();
        try {
            data.put("EQ_ID",""+androidId);
            data.put("HR_MIN", ""+0);
            data.put("HR_MAX", ""+finalheartrate);
            data.put("DT",""+formatter.format(date));
            jobj.put("ID", "HR");
            jobj.put("DATA", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ApiInterface.getRequestApiInterface().sendData(jobj.toString()).enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                if (response.isSuccessful() && response.body() != null)
                {
                    if (response.body().getResult())
                    {
                        Log.d("Testing","Heart Sent");
                    }
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Log.d("Error ",""+t.getMessage());
            }
        });
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            int heart_rate = (int) event.values[0];

            if (heart_rate>=40 && heart_rate<=220)
                finalheartrate = heart_rate;

            Log.d("data2","->rate : "+heart_rate);
        } else {
            // Do nothing
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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

/*    void sendData(String heartrate) {
        DataClient dataclient = Wearable.getDataClient(getApplicationContext());
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/Haenyeo_Health");
        putDataMapReq.getDataMap().putString("HeartRate", heartrate);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = dataclient.putDataItem(putDataReq);
    }*/
}