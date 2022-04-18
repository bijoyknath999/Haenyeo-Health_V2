package com.HHMS;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.wear.widget.BoxInsetLayout;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UniversalActivity extends AppCompatActivity implements MqttCallback {

    private TextView SSAIDTEXT, PROCESSINGTEXT,DriverID;
    private LinearLayout Layout1, Layout2;
    private BoxInsetLayout LayoutBg;
    private AppCompatButton RegBtn, StartBn;
    private String androidId, driverid = "0";

    private final String serverUrl   = "tcp://220.118.147.52:7883";
    private final String clientId    = "RW_WATCH_01";
    private String message;  // example data
    //final String tenant      = "<<tenant_ID>>";
    private final String username    = "rwit";
    private final String password    = "5be70721a1a11eae0280ef87b0c29df5aef7f248";
    private final String topic = "RW/JD/DI"; //  RW/JD/DI TODO chanag!!!!
    private final String topic2 = "RW/JD/DS"; //  RW/JD/DI TODO chanag!!!!
    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    private CountDownTimer cdt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal);

        SSAIDTEXT = findViewById(R.id.universal_ssaid_text);
        PROCESSINGTEXT = findViewById(R.id.universal_processing_text);
        RegBtn = findViewById(R.id.universal_registration_btn);
        DriverID = findViewById(R.id.universal_driverid_text);
        StartBn = findViewById(R.id.universal_start_btn);


        LayoutBg = findViewById(R.id.universal_layout_bg);
        Layout1 = findViewById(R.id.universal_layout_1);
        Layout2 = findViewById(R.id.universal_layout_2);


        androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SSAIDTEXT.setText(""+androidId);

        message = "ID || DI ^^ EQID || "+androidId+" ^^ TS || 1648099351515";

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
            mqttClient.subscribe(topic2);
        } catch (MqttException e) {
            e.printStackTrace();
        }

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
                        mqttClient.publish(topic, message.getBytes(), 0, false);
                        System.out.println("Sending done...");

                        driverid = "-1";
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

        cdt = new CountDownTimer(3000,1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                CheckDriverID();
                cdt.start();
            }
        };

        cdt.start();
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
        System.out.println("topic :"+topic);
        System.out.println("message :"+message);
        String messageStr = String.valueOf(message);
        String messageStr2 = messageStr.substring(messageStr.indexOf("HNID || ")+8);
        String firstWord = messageStr2.replaceAll(" ", "*");
        driverid = firstWord.substring(0, firstWord.indexOf("*^"));
    }

    private void CheckDriverID() {

        if (!driverid.equals("-1") && !driverid.equals("0"))
        {
            LayoutBg.setBackgroundColor(getResources().getColor(R.color.color10));
            Layout1.setVisibility(View.VISIBLE);
            Layout2.setVisibility(View.GONE);
            DriverID.setText(""+driverid);
            StartBn.setVisibility(View.VISIBLE);
        }
        else if (driverid.equals("0"))
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
            DriverID.setText("Not Ready");
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
}