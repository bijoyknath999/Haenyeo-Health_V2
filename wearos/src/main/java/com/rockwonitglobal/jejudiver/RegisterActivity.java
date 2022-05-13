package com.rockwonitglobal.jejudiver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.HealthTrackerException;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.HealthTrackerType;
import com.samsung.android.service.health.tracking.data.ValueKey;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity implements MqttCallback {

    private TextView SSAIDTEXT;
    private AppCompatButton RegBtn;
    private final String serverUrl   = "ssl://iot.shovvel.com:47883";
    private String clientId    = "";
    private String message;  // example data
    //final String tenant      = "<<tenant_ID>>";
    private final String username    = "rwit";
    private final String password    = "5be70721a1a11eae0280ef87b0c29df5aef7f248";
    private final String topic1 = "RW/JD/DI"; //  RW/JD/DI TODO chanag!!!!
    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    private String androidId, diverid = "0";
    private int finaldiverid, SaveID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        SSAIDTEXT = findViewById(R.id.register_ssaid_text);
        RegBtn = findViewById(R.id.register_registration_btn);

        androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        clientId = UUID.randomUUID().toString();


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
            mqttClient.subscribe("RW/JD/DS",0);
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
                        mqttClient.publish(topic1, message.getBytes(), 0, false);
                        System.out.println("Sending done...");
                        //startActivity(new Intent(RegisterActivity.this,UniversalActivity.class)); //remove code
                        Toast.makeText(RegisterActivity.this, "sending.....", Toast.LENGTH_SHORT).show(); //<== add toast
                    }
                    else
                        System.out.println("Failed To Send.......");


                } catch (MqttException e) {
                    System.out.println("Error :"+e.getMessage());
                }
            }
        });
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

        if ("RW/JD/DS".equals(topic))
        {
            String messageStr = new String(message.getPayload(),"UTF-8");
            String diverID = Tools.getData(messageStr, "HNID");
            String SSAID = Tools.getData(messageStr, "EQID");


            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            if(SSAID.equals(androidId) && !"".equals(androidId))
            {
                if("-1".equals(diverID))
                {
                    startActivity(new Intent(RegisterActivity.this,UniversalActivity.class));
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
            } catch (MqttException e) {
                e.printStackTrace();
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

}