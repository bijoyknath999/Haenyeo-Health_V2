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
import java.util.UUID;

public class UniversalActivity extends AppCompatActivity implements MqttCallback, ConnectionListener {

    private TextView SSAIDTEXT,DiverID;
    private BoxInsetLayout LayoutBg;
    private AppCompatButton HeartRateBtn,Sp02Btn;
    private String androidId, diverid = "0";

    private String finaldiverid, SaveID;

    private final String serverUrl   = "ssl://iot.shovvel.com:47883";
    private String clientId    = "";
    private String message;  // example data
    //final String tenant      = "<<tenant_ID>>";
    private final String username    = "rwit";
    private final String password    = "5be70721a1a11eae0280ef87b0c29df5aef7f248";
    private final String topic2 = "RW/JD/DS"; //  RW/JD/DI TODO chanag!!!!
    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    private CountDownTimer cdt;

    private String TAG = "Test";

    private HealthTracker spo2Tracker = null;
    private HealthTrackingService healthTrackingService = null;
    private final Handler handler = new Handler(Looper.myLooper());
    Handler handler2 = new Handler();
    private int prevStatus = -100;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal);

        SSAIDTEXT = findViewById(R.id.universal_ssaid_text);
        DiverID = findViewById(R.id.universal_diverid_text);
        HeartRateBtn = findViewById(R.id.universal_heart_rate_btn);
        Sp02Btn = findViewById(R.id.universal_sp02_btn);


        LayoutBg = findViewById(R.id.universal_layout_bg);

        androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        clientId = UUID.randomUUID().toString();

        SSAIDTEXT.setText(""+androidId);


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
            mqttClient.subscribe(topic2,0);
        } catch (MqttException e) {
            e.printStackTrace();
        }




        HeartRateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String diveid_ = Tools.getID("diverid",UniversalActivity.this);
                if (!diveid_.equals("0") && !diveid_.equals("-1")) {
                    if (cdt!=null)
                        cdt.cancel();
                    try {
                        mqttClient.disconnect();
                        mqttClient.close();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(UniversalActivity.this,HomeActivity.class);
                    intent.putExtra("options",1);
                    startActivity(intent);
                    finish();
                }
                else
                    Toast.makeText(UniversalActivity.this, "Please Re-register.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void LoadFunc() {
        SaveID = Tools.getID("diverid",UniversalActivity.this);
        if (!SaveID.equals("0") && !SaveID.equals("-1"))
        {
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color10));
            DiverID.setText(""+SaveID);
            HeartRateBtn.setVisibility(View.VISIBLE);
            Sp02Btn.setVisibility(View.VISIBLE);
            healthTrackingService = new HealthTrackingService(this, getApplicationContext());
            healthTrackingService.connectService();
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

        System.out.println(""+topic);

        if (topic.equals(topic2))
        {
            String messageStr = new String(message.getPayload(),"UTF-8");
            String diverID = Tools.getData(messageStr, "HNID");
            String SSAID = Tools.getData(messageStr, "EQID");

            System.out.println("diver id : "+diverID);
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            if(SSAID.equals(androidId) && !"".equals(androidId))
            {
                Tools.saveID("diverid", diverID, UniversalActivity.this);

                if(!"-1".equals(diverID))
                {
                    Intent intent = new Intent(UniversalActivity.this,HomeActivity.class);
                    intent.putExtra("options",1);
                    startActivity(intent);
                    finish();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

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


        if(spo2Tracker != null) {
            spo2Tracker.unsetEventListener();
        }
        handler.removeCallbacksAndMessages(null);
        if(healthTrackingService != null) {
            healthTrackingService.disconnectService();
        }

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

    private void getSp02Value() {
        prevStatus = -100;
        Toast.makeText(UniversalActivity.this, "SpO2 measuring", Toast.LENGTH_SHORT).show();
        handler.post(() -> {
            spo2Tracker.setEventListener(trackerEventListener);
        });
    }

    private void CheckDriverID() {

        System.out.println(""+diverid);

        finaldiverid = Tools.getID("diverid",UniversalActivity.this);

        if (!finaldiverid.equals("0") && !finaldiverid.equals("-1"))
        {
            Tools.saveID("diverid", finaldiverid, UniversalActivity.this);
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color10));
            DiverID.setText(""+finaldiverid);
            HeartRateBtn.setVisibility(View.VISIBLE);
            Sp02Btn.setVisibility(View.VISIBLE);        }
        else
        {
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color9));
            DiverID.setText("Not Ready");
            HeartRateBtn.setVisibility(View.GONE);
            Sp02Btn.setVisibility(View.GONE);
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

        try {
            spo2Tracker = healthTrackingService.getHealthTracker(HealthTrackerType.SPO2);
        } catch (final IllegalArgumentException e) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show()
            );
            finish();
        }

        Sp02Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String diveid_ = Tools.getID("diverid",UniversalActivity.this);
                if (!diveid_.equals("0") && !diveid_.equals("-1")) {
                    if (cdt!=null)
                        cdt.cancel();
                    try {
                        mqttClient.disconnect();
                        mqttClient.close();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    getSp02Value();
                }
                else
                    Toast.makeText(UniversalActivity.this, "Please Re-register.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionEnded() {
        //Toast.makeText(this, "Ended!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(HealthTrackerException e) {
        if(e.hasResolution()) {
            e.resolve(UniversalActivity.this);
        }
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Unable to connect to HSP", Toast.LENGTH_LONG).show()
        );
        finish();
    }

    private final HealthTracker.TrackerEventListener trackerEventListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.size() != 0) {
                Log.i(TAG, "List Size : "+list.size());
                for(DataPoint dataPoint : list) {
                    int status = dataPoint.getValue(ValueKey.SpO2Set.STATUS);
                    if (prevStatus != status) {
                        prevStatus = status;
                        runOnUiThread(() -> {
                            if (status == 2) {
                                Toast.makeText(UniversalActivity.this, "success...", Toast.LENGTH_SHORT).show();
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
                            Tools.saveField("sp02_value",dataPoint.getValue(ValueKey.SpO2Set.SPO2),UniversalActivity.this);
                            if (dataPoint.getValue(ValueKey.SpO2Set.SPO2)>0) {
                                handler2.removeCallbacksAndMessages(null);
                                if(healthTrackingService != null) {
                                    healthTrackingService.disconnectService();
                                }
                                Intent intent = new Intent(UniversalActivity.this,HomeActivity.class);
                                intent.putExtra("options",2);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
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