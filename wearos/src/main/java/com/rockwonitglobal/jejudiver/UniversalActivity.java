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

public class UniversalActivity extends AppCompatActivity implements MqttCallback {

    private TextView SSAIDTEXT,DiverID;
    private BoxInsetLayout LayoutBg;
    private AppCompatButton HeartRateBtn,Sp02Btn;
    private String androidId, diverid = "0";

    private int finaldiverid, SaveID;

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

        message = "ID || DS ^^ EQID || "+androidId+" ^^ HNID || -1 ^^ TS || "+getCurrentTimestamp();

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


        if (mqttClient.isConnected()) {
            try {
                mqttClient.publish(topic2, message.getBytes(), 0, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }



        HeartRateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int diveid_ = Tools.getID("diverid",UniversalActivity.this);
                if (diveid_ > 0) {
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
                }
                else
                    Toast.makeText(UniversalActivity.this, "Please Re-register.", Toast.LENGTH_SHORT).show();
            }
        });

        Sp02Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int diveid_ = Tools.getID("diverid",UniversalActivity.this);
                if (diveid_ > 0) {
                    if (cdt!=null)
                        cdt.cancel();
                    try {
                        mqttClient.disconnect();
                        mqttClient.close();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(UniversalActivity.this,HomeActivity.class);
                    intent.putExtra("options",2);
                    startActivity(intent);
                }
                else
                    Toast.makeText(UniversalActivity.this, "Please Re-register.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void LoadFunc() {
        SaveID = Tools.getID("diverid",UniversalActivity.this);
        if (SaveID>0)
        {
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color10));
            DiverID.setText(""+SaveID);
            HeartRateBtn.setVisibility(View.VISIBLE);
            Sp02Btn.setVisibility(View.VISIBLE);
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
            DiverID.setText(""+finaldiverid);
            HeartRateBtn.setVisibility(View.VISIBLE);
            Sp02Btn.setVisibility(View.VISIBLE);        }
        else
        {
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color9));
            DiverID.setText("Not Ready");
            HeartRateBtn.setVisibility(View.GONE);
            Sp02Btn.setVisibility(View.GONE);        }
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
}