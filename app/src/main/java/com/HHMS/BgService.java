package com.HHMS;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.SEND_SMS;

import android.annotation.SuppressLint;
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
import android.telephony.TelephonyManager;
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
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BgService extends Service implements DataClient.OnDataChangedListener, KeyListener {


    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private SharedPreferences sharedPreferences;
    private String IMEINumber;




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

    @Override
    // execution of the service will
    // stop on calling this method
    public void onDestroy() {
        stopall();
        super.onDestroy();
    }

    public  void stopall()
    {
        Wearable.getDataClient(this).removeListener(this);

        stopForeground(true);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


   /* //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        startService(new Intent(HomeActivity.this,BgService.class));
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }*/


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
                        SendHeartRateServer(Integer.parseInt(HeartData));
                    }
                    if (sos)
                    {
                        sosrunWear(lat,lon);
                        SendSOSServer();
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
            getApplicationContext().startActivity(callIntent);
        }
    }


    private void SendHeartRateServer(int heartrate)
    {
        JSONObject data = new JSONObject();

        JSONObject HRval = new JSONObject();
        try {
            data.put("IMEI", "");
            data.put("HR_MIN", heartrate);
            data.put("HR_MAX", heartrate);

            HRval.put("ID", "HR");
            HRval.put("DATA", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ApiInterface.getRequestApiInterface().sendData(HRval.toString()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null)
                {
                    Log.d("HeartRate ",""+response.body());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("Error ",""+t.getMessage());
            }
        });
    }

    private void SendSOSServer()
    {
        JSONObject data = new JSONObject();

        JSONObject jobj = new JSONObject();
        try {
            data.put("IMEI", "");
            jobj.put("ID", "SO");
            jobj.put("DATA", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ApiInterface.getRequestApiInterface().sendData(jobj.toString()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null)
                {
                    Log.d("SOS ",""+response.body());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("Error ",""+t.getMessage());
            }
        });
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