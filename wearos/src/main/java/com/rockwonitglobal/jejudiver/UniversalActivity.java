package com.rockwonitglobal.jejudiver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.wear.widget.BoxInsetLayout;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import com.samsung.android.service.health.tracking.ConnectionListener;
import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.HealthTrackerException;
import com.samsung.android.service.health.tracking.HealthTrackingService;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.HealthTrackerType;
import com.samsung.android.service.health.tracking.data.ValueKey;

import java.util.Date;
import java.util.List;

public class UniversalActivity extends AppCompatActivity implements MqttCallback, ConnectionListener {

    private TextView SSAIDTEXT, PROCESSINGTEXT,DiverID;
    private LinearLayout Layout1, Layout2;
    private BoxInsetLayout LayoutBg;
    private AppCompatButton RegBtn, StartBn;
    private String androidId, diverid = "0";

    private int finaldiverid, SaveID;

    private final String serverUrl   = "tcp://220.118.147.52:7883";
    private final String clientId    = "RW_WATCH_01";
    private String message;  // example data
    //final String tenant      = "<<tenant_ID>>";
    private final String username    = "rwit";
    private final String password    = "5be70721a1a11eae0280ef87b0c29df5aef7f248";
    private final String[] topic = {"RW/JD/DS","RW/JD/DI"};
    private final String topic1 = "RW/JD/DI"; //  RW/JD/DI TODO chanag!!!!
    private final String topic2 = "RW/JD/DS"; //  RW/JD/DI TODO chanag!!!!
    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    private CountDownTimer cdt;
    private HealthTracker spo2Tracker = null;
    private HealthTrackingService healthTrackingService = null;
    private final Handler handler = new Handler(Looper.myLooper());

    private String TAG = "Test";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal);

        SSAIDTEXT = findViewById(R.id.universal_ssaid_text);
        PROCESSINGTEXT = findViewById(R.id.universal_processing_text);
        RegBtn = findViewById(R.id.universal_registration_btn);
        DiverID = findViewById(R.id.universal_diverid_text);
        StartBn = findViewById(R.id.universal_start_btn);


        LayoutBg = findViewById(R.id.universal_layout_bg);
        Layout1 = findViewById(R.id.universal_layout_1);
        Layout2 = findViewById(R.id.universal_layout_2);

        androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SSAIDTEXT.setText(""+androidId);

        message = "ID || DI ^^ EQID || "+androidId+" ^^ TS || "+getCurrentTimestamp();

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
//            mqttClient.subscribe(topic1,0);
//            mqttClient.subscribe(topic2,0);
            mqttClient.subscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }


        StartBn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int diveid_ = Tools.getID("diverid",UniversalActivity.this);
                if (diveid_ > 0) {
                    try {
                        mqttClient.disconnect();
                        mqttClient.close();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Tools.saveID("datasend", 1, UniversalActivity.this);
                    startActivity(new Intent(UniversalActivity.this, HomeActivity.class));
                    finish();
                }
                else
                    Toast.makeText(UniversalActivity.this, "Please Re-register.", Toast.LENGTH_SHORT).show();
            }
        });

        RegBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    if (!mqttClient.isConnected()) {
                        mqttClient.connect();
                    }

                    if (mqttClient.isConnected())
                    {
                        System.out.println("Sending message...");
                        mqttClient.publish(topic1, message.getBytes(), 0, false);
                        System.out.println("Sending done...");

                        diverid = "-1";
                        LayoutBg.setBackgroundColor(getResources().getColor(R.color.color9));
                        Layout2.setVisibility(View.GONE);
                        Layout1.setVisibility(View.VISIBLE);
                    }
                    else
                        System.out.println("Failed To Send.......");


                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });


        healthTrackingService = new HealthTrackingService(this, getApplicationContext());
    }

    private void LoadFunc() {
        SaveID = Tools.getID("diverid",UniversalActivity.this);
        if (SaveID>0)
        {
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color10));
            Layout1.setVisibility(View.VISIBLE);
            Layout2.setVisibility(View.GONE);
            DiverID.setText(""+SaveID);
            StartBn.setVisibility(View.VISIBLE);
        }

        cdt = new CountDownTimer(3000,1000) {
            public void onTick(long millisUntilFinished) {
                System.out.println(" "+millisUntilFinished);
            }
            public void onFinish() {
                CheckDriverID();
                cdt.start();
            }
        };

        cdt.start();
    }


    public static String getCurrentTimestamp() {
        Date date = new Date();
        //This method returns the time in millis
        long timeMilli = date.getTime();
        return String.valueOf(timeMilli);
    }

    @Override
    public void connectionLost(Throwable cause) {
        //System.out.println("Connection lost 2! " + cause.getMessage());
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

        System.out.println("topic :"+topic);

        if (topic.equals(topic2))
        {
            System.out.println("topic :"+topic);
            System.out.println("message :"+message);
            String messageStr = String.valueOf(message);
            String messageStr2 = messageStr.substring(messageStr.indexOf("HNID || ")+8);
            String firstWord = messageStr2.replaceAll(" ", "*");
            diverid = firstWord.substring(0, firstWord.indexOf("*^"));
            Tools.saveID("diverid", Integer.parseInt(diverid), UniversalActivity.this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        healthTrackingService.connectService();
        if (!mqttClient.isConnected()) {
            try {
                mqttClient.connect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        LoadFunc();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cdt!=null)
            cdt.cancel();

        if (mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

    private void CheckDriverID() {

        System.out.println(""+diverid);

        finaldiverid = Tools.getID("diverid",UniversalActivity.this);

        if (finaldiverid>0)
        {
            Tools.saveID("diverid", finaldiverid, UniversalActivity.this);
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color10));
            Layout1.setVisibility(View.VISIBLE);
            Layout2.setVisibility(View.GONE);
            DiverID.setText(""+finaldiverid);
            StartBn.setVisibility(View.VISIBLE);
        }
        else if (finaldiverid == 0)
        {
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color8));
            Layout1.setVisibility(View.GONE);
            Layout2.setVisibility(View.VISIBLE);
            StartBn.setVisibility(View.GONE);
        }
        else
        {
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color9));
            Layout1.setVisibility(View.VISIBLE);
            Layout2.setVisibility(View.GONE);
            DiverID.setText("Not Ready");
            StartBn.setVisibility(View.GONE);
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

    @Override
    public void onConnectionSuccess() {

        //Toast.makeText(this, "Connected!!", Toast.LENGTH_SHORT).show();

        try {
            spo2Tracker = healthTrackingService.getHealthTracker(HealthTrackerType.SPO2);
        } catch (final IllegalArgumentException e) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show()
            );
            finish();
        }
    }

    @Override
    public void onConnectionEnded() {
        Toast.makeText(this, "Ended!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(HealthTrackerException e) {

        //Toast.makeText(this, "Failed : "+e.getMessage(), Toast.LENGTH_SHORT).show();

    }

    private final HealthTracker.TrackerEventListener trackerEventListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.size() != 0) {
                Log.i(TAG, "List Size : "+list.size());
                for(DataPoint dataPoint : list) {
                    int status = dataPoint.getValue(ValueKey.SpO2Set.STATUS);
                    Log.i(TAG, "Status : " + status);
                    runOnUiThread(() -> {
                        if (status == 2) {
                            if(spo2Tracker != null) {
                                spo2Tracker.unsetEventListener();
                            }
                            handler.removeCallbacksAndMessages(null);
                        }
                        else if (status == 0) {
                        }
                        else if (status == -4){
                            Toast.makeText(getApplicationContext(), "Moving : " + status, Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(getApplicationContext(), "Low Signal : " + status, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Log.i(TAG, "onDataReceived List is zero");
            }
        }

        @Override
        public void onFlushCompleted() {
            Log.i(TAG, " onFlushCompleted called");
        }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            Log.i(TAG, " onError called");
            if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        "Permissions Check Failed", Toast.LENGTH_SHORT).show());
            }
            if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        "SDK Policy denied", Toast.LENGTH_SHORT).show());
            }
        }
    };
}