package com.rockwonitglobal.jejudiver;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.SEND_SMS;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.HHMS.models.Result;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BgService extends Service implements DataClient.OnDataChangedListener {


    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private SharedPreferences sharedPreferences;
    private String datapath = "/message_path";
    private String message = "0";



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

                IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
                BgService.MessageReceiver messageReceiver = new BgService.MessageReceiver();
                LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
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
        BgService.MessageReceiver messageReceiver = new BgService.MessageReceiver();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
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
                    String HeartData = dataMapItem.getDataMap().getString("HeartRate");
                    int heartrate = Integer.parseInt(HeartData);
                    Log.d("Testing",""+heartrate);
                    if (heartrate>=40 && heartrate <= 220)
                    {
                        //SendHeartRateServer(Integer.parseInt(HeartData));
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

    private void sosrun() {
        GpsTracker gpsTracker = new GpsTracker(getApplicationContext());
        sharedPreferences = getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        String number = sharedPreferences.getString("number","");
        if (!number.isEmpty()) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, "Emergency SOS\nYou're receiving this message this contact has listed you as an emergency contact.\n" +
                    "https://maps.google.com/?q=" + gpsTracker.getLatitude() + "," + gpsTracker.getLongitude(), null, null);
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(callIntent);
        }
    }


    /*private void SendHeartRateServer(int heartrate)
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
            data.put("HR_MAX", ""+heartrate);
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
                        Log.d("Testing","Heart Rate Sent.");
                    }
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Log.d("Testing ",""+t.getMessage());
            }
        });
    }*/

    private void SendSOSServer()
    {
        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date();

        JSONObject data = new JSONObject();

        JSONObject jobj = new JSONObject();
        try {
            data.put("EQ_ID",""+androidId);
            data.put("DT",""+formatter.format(date));
            jobj.put("ID", "SO");
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
                        Log.d("Testing","SOS Sent bg.");
                    }
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Log.d("Error ",""+t.getMessage());
            }
        });
    }
    //setup a broadcast receiver to receive the messages from the wear device via the listenerService.
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");


            if(message.equals("0")) {
                SendMessage();
                sosrun();
                SendSOSServer();
            }
            else
            {
                SendMessage();
                double lat = Double.parseDouble(message.substring(0, message.indexOf('_')));
                double lon = Double.parseDouble(message.substring(message.indexOf("_") + 1));
                sosrunWear(lat,lon);
                SendSOSServer();
            }
        }
    }

    private void SendMessage()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //first get all the nodes, ie connected wearable devices.
                Task<List<Node>> nodeListTask =
                        Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
                try {
                    // Block on a task and get the result synchronously (because this is on a background
                    // thread).
                    List<Node> nodes = Tasks.await(nodeListTask);



                    //Now send the message to each device.
                    for (Node node : nodes) {
                        Task<Integer> sendMessageTask =
                                Wearable.getMessageClient(getApplicationContext()).sendMessage(node.getId(), datapath, message.getBytes());

                        try {
                            // Block on a task and get the result synchronously (because this is on a background
                            // thread).
                            Integer result = Tasks.await(sendMessageTask);
                            Log.v("Testing", "SendThread: message send to " + node.getDisplayName());

                        } catch (ExecutionException exception) {
                            Log.e("Testing", "Task failed: " + exception);

                        } catch (InterruptedException exception) {
                            Log.e("Testing", "Interrupt occurred: " + exception);
                        }

                    }

                } catch (ExecutionException exception) {
                    Log.e("Testing", "Task failed: " + exception);

                } catch (InterruptedException exception) {
                    Log.e("Testing", "Interrupt occurred: " + exception);
                }
            }
        }).start();

    }
}