package com.HHMS;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.SEND_SMS;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.snackbar.Snackbar;

public class BgService extends Service implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, KeyListener {


    public static GoogleApiClient googleClient;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private SharedPreferences sharedPreferences;




    // execution of service will start
    // on calling this method
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (intent.getAction().equals("start")) {
            googleClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            createNotificationChannel();
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("해녀건강")
                    .setContentText("Health Service is running")
                    .setSmallIcon(R.drawable.ic_notifications)
                    .build();
            startForeground(1, notification);
            //do heavy work on a background thread
            //stopSelf();
            Log.d("Bg","Bging");


            googleClient.connect();
        }
        else if (intent.getAction().equals("stop")) {
            stopall();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "해녀건강 Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    // execution of the service will
    // stop on calling this method
    public void onDestroy() {
        stopall();
        super.onDestroy();
    }

    public  void stopall()
    {
        if (googleClient!=null)
        {
            Wearable.DataApi.removeListener(googleClient, this);
            googleClient.disconnect();
            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //on successful connection to play services, add data listner
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(googleClient, this);
    }

    //on suspended connection, remove play services
    public void onConnectionSuspended(int cause) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

   /* //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        startService(new Intent(HomeActivity.this,BgService.class));
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }*/

    //On failed connection to play services, remove the data listener
    public void onConnectionFailed(ConnectionResult result) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

    //function triggered every time there's a data change event
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("Tag","changed");
        for(DataEvent event: dataEvents){

            //data item changed
            if(event.getType() == DataEvent.TYPE_CHANGED){

                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);

                if(item.getUri().getPath().equals("/Haenyeo_Health")){
                    double lat = dataMapItem.getDataMap().getDouble("lat");
                    double lon = dataMapItem.getDataMap().getDouble("lon");
                    boolean sos = dataMapItem.getDataMap().getBoolean("sos");
                    Log.d("Tag","->lat :"+lat+", lon :"+lon);
                    String HeartData = dataMapItem.getDataMap().getString("HeartRate");
                    int heartrate = Integer.parseInt(HeartData);
                    if (heartrate>=40 && heartrate <= 220)
                    {

                    }
                    if (sos)
                    {
                        Log.d("Tag","SOSing");
                        sosrunWear(lat,lon);
                    }
                }
                else
                {

                }
            }
        }
    }

    private void sosrunWear(double lat, double lon) {
        sharedPreferences = getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        String number = sharedPreferences.getString("number","");
        if (!number.isEmpty()) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, "Emergency SOS\nYou're receiving this message this contact has listed you as an emergency contact.\n" +
                    "https://maps.google.com/?q=" + lat + "," + lon, null, null);
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(callIntent);
        }
    }

    @Override
    public int getInputType() {
        return 0;
    }

    @Override
    public boolean onKeyDown(View view, Editable editable, int i, KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_POWER) {
            i++;
            if(i==2){
                Toast.makeText(getApplicationContext(),"SOS Data Send Successfully !!", Toast.LENGTH_SHORT).show();
                i=0;
            }

        }
        return true;
    }

    @Override
    public boolean onKeyUp(View view, Editable editable, int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onKeyOther(View view, Editable editable, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void clearMetaKeyState(View view, Editable editable, int i) {

    }
}