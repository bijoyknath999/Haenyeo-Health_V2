package com.rockwonitglobal.jejudiver;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.wearable.DataClient;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Calendar;
import java.util.UUID;

public class BgService extends Service implements SensorEventListener, LocationListener, MqttCallback {
    private SensorManager sensorService;
    private Sensor heartSensor;
    private String finaldiverid;
    private int finalheartrate;
    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    private String androidId;
    private final String serverUrl   = "ssl://iot.shovvel.com:47883";
    private String clientId = "";
    private final String username    = "rwit";
    private final String password    = "5be70721a1a11eae0280ef87b0c29df5aef7f248";
    private final String topic1 = "RW/JD/HD";
    private final String topic4 = "RW/JD/DS";
    private String messagehearrate;

    Handler handler2 = new Handler();
    Runnable runnable;
    int delay = 60000;

    private LocationManager locationManager;
    private Location location;
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    private double latitude = 0, longitude = 0;

    public static final String CHANNEL_ID = "ForegroundServiceChannel";



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        System.out.println("Bging");

        if (intent.getAction().equals("start")) {

            createNotificationChannel();
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Woman Sea Diver")
                    .setContentText("Health Service is running")
                    .setSmallIcon(R.drawable.ic_cc_settings_button_center)
                    .build();
            startForeground(1, notification);

            androidId = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            clientId = UUID.randomUUID().toString();

            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(true);
            mqttConnectOptions.setKeepAliveInterval(1000);
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setConnectionTimeout(1000);
            mqttConnectOptions.setUserName(username);
            mqttConnectOptions.setPassword(password.toCharArray());

            try {
                mqttClient = new MqttClient(serverUrl, clientId, new MemoryPersistence());
                mqttClient.setCallback(this);
                mqttClient.connect(mqttConnectOptions);
                mqttClient.subscribe(topic4);  //only receive RW/JD/DS data
            } catch (MqttException e) {
                e.printStackTrace();
            }
            GpsTracker gpsTracker = new GpsTracker(getApplicationContext());
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();
            getHeartRate();
            RunTimer();

        }
        else if (intent.getAction().equals("stop")) {
            stopForeground(true);
            stopSelf();
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

    void getHeartRate() {
        sensorService = (SensorManager) getSystemService(SENSOR_SERVICE);
        heartSensor = sensorService.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorService.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
        handler2.removeCallbacksAndMessages(null);
        if (mqttClient!=null)
        {
            if (mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }

        if (sensorService!=null)
        {
            sensorService.unregisterListener(this);
        }

        System.out.println("Destroyed!!");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            int heart_rate = (int) event.values[0];
            if (heart_rate>=40 && heart_rate<=220)
            {
                finalheartrate = heart_rate;
                Tools.saveField("heart_rate",finalheartrate,getApplicationContext());
            }
        }
        else {
            // Do nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost! " + cause.getMessage());
        if (!mqttClient.isConnected()) {
            try {
                mqttClient.connect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if ("RW/JD/DS".equals(topic4))
        {
            String messageStr = new String(message.getPayload(),"UTF-8");
            String diverID = Tools.getData(messageStr, "HNID");
            String SSAID = Tools.getData(messageStr, "EQID");

            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            if(SSAID.equals(androidId) && !"".equals(androidId))
            {
                Tools.saveID("diverid", diverID,getApplicationContext());
                if (diverID.equals("-1"))
                {

                }
                else if (!diverID.equals("-1"))
                    finaldiverid = diverID;
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
        } catch (MqttException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String getCurrentTimestamp() {
        //creating Calendar instance
        Calendar calendar = Calendar.getInstance();
        //Returns current time in millis
        long timeMilli = calendar.getTimeInMillis();
        return String.valueOf(timeMilli);
    }

    private void RunTimer() {
        handler2.postDelayed(runnable = new Runnable() {
            public void run() {
                System.out.println("Testing");
                handler2.postDelayed(runnable, delay);
                PublishDataWithHearRate();
            }
        }, delay);
    }

    private void PublishDataWithHearRate() {
        try {
            if (latitude!=0.0)
                messagehearrate = "ID || HD ^^ EQID || "+androidId+" ^^ HNID || "+finaldiverid+" ^^ LAT || "+latitude+" ^^ LNG || "+longitude+" ^^ HR || "+finalheartrate+" ^^ TS || "+getCurrentTimestamp();

            if (!mqttClient.isConnected()) {
                mqttClient.connect();
            }

            if (mqttClient.isConnected())
            {
                System.out.println("Sending heart rate...");
                mqttClient.publish(topic1, messagehearrate.getBytes(), 0, false);
                System.out.println("Sending done...");
            }
            else
                System.out.println("Failed To Send.......");


        } catch (MqttException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Testing2");

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }
}