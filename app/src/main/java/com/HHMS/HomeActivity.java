package com.HHMS;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.SEND_SMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.HHMS.models.Result;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class HomeActivity extends AppCompatActivity implements DataClient.OnDataChangedListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    String HeartData;
    private TextView tvHeartRate, tvwishes,tvnoheart;
    private ProgressBar progressBar;
    private ImageView StatusImage;
    private LinearLayout SOS;
    private BottomNavigationView navigationView;
    private RelativeLayout MainLayout;
    private SharedPreferences sharedPreferences;
    private RequestChecker requestChecker;
    private GoogleApiClient googleClient;
    private String datapath = "/message_path";
    private String message = "0";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();

        getSupportActionBar().setTitle("Home");

        tvHeartRate = (TextView) findViewById(R.id.home_heart_rate_text);
        tvwishes = findViewById(R.id.home_heart_wishes);
        tvnoheart = findViewById(R.id.home_heart_rate_no_text);
        progressBar = findViewById(R.id.home_heart_rate_progress);
        StatusImage = findViewById(R.id.home_device_status);
        SOS = findViewById(R.id.home_sos);
        MainLayout = findViewById(R.id.home_layout);
        sharedPreferences = getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);

        MQTT_PUB2.main();
        RwMqttClient mqttClient1 = new RwMqttClient();
        mqttClient1.init();


        Log.d("Testing", ""+mqttClient1.isConnected());
        try {
            mqttClient1.connect();
        } catch (MqttException e) {
            e.printStackTrace();
        }

        Log.d("Testing2", ""+mqttClient1.isConnected());

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if(timeOfDay >= 0 && timeOfDay < 12){
            tvwishes.setText("Good Morning !");
        }else if(timeOfDay >= 12 && timeOfDay < 16){
            tvwishes.setText("Good Afternoon !");
        }else if(timeOfDay >= 16 && timeOfDay < 21){
            tvwishes.setText("Good Evening !");
        }else if(timeOfDay >= 21 && timeOfDay < 24){
            tvwishes.setText("Good Night !");
        }


        //data layer
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        navigationView = findViewById(R.id.navigation);

        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.navigation_home:
                        Toast.makeText(HomeActivity.this, "Home", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.navigation_location:
                        SendLocationServer();
                        return true;
                    case R.id.navigation_settings:
                        startActivity(new Intent(HomeActivity.this,SettingsActivity.class));
                        return true;
                }

                return true;
            }
        });


        SOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sosrun();
                SendSOSServer();
            }
        });

        requestChecker = new RequestChecker(HomeActivity.this);
        if (!requestChecker.CheckingPermissionIsEnabledOrNot())
            requestChecker.RequestMultiplePermission();


        googleClient.connect();
        IsConnected();

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

    }


    private void IsConnected(){
        Wearable.NodeApi.getConnectedNodes(googleClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                List<Node> connectedNodes =
                        getConnectedNodesResult.getNodes();
                Log.d("Tag","node"+connectedNodes.size());
                if (connectedNodes.size()>0)
                    StatusImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_online));
                else
                    StatusImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_offline));
            }
        });
    }


    //on successful connection to play services, add data listner
    public void onConnected(Bundle connectionHint) {
        Wearable.getDataClient(this).addListener(this);
        Log.d("Tag",""+googleClient.isConnected());
    }


    //on suspended connection, remove play services
    public void onConnectionSuspended(int cause) {
        Wearable.getDataClient(this).removeListener(this);
        Log.d("Tag","Suspend"+cause);
    }

    //On failed connection to play services, remove the data listener
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("Tag","error :"+result.getErrorMessage());
        Wearable.getDataClient(this).removeListener(this);
    }

    private void SendSOSServer()
    {
        GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);

        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date();

        JSONObject data = new JSONObject();

        JSONObject jobj = new JSONObject();
        try {
            data.put("EQ_ID",""+androidId);
            data.put("LAT", ""+gpsTracker.getLatitude());
            data.put("LNG", ""+gpsTracker.getLongitude());
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
                        Log.v("Testing","SOS Sent from phone");
                    }
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Log.d("Error ",""+t.getMessage());
            }
        });
    }

   /* private void SendHeartRateServer(int heartrate)
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
                        Log.v("Testing","HeartRate Sent");
                    }
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Log.d("Error ",""+t.getMessage());
            }
        });
    }*/

    private void SendLocationServer()
    {
        GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);

        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date();

        JSONObject data = new JSONObject();

        JSONObject jobj = new JSONObject();
        try {
            data.put("EQ_ID",""+androidId);
            data.put("LAT", ""+gpsTracker.getLatitude());
            data.put("LNG", ""+gpsTracker.getLongitude());
            data.put("DT",""+formatter.format(date));
            jobj.put("ID", "LP");
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
                        Log.v("Testing","Location Sent from phone");
                    }
                    startActivity(new Intent(HomeActivity.this, MapsActivity.class));
                }
                else
                {
                    startActivity(new Intent(HomeActivity.this, MapsActivity.class));

                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                startActivity(new Intent(HomeActivity.this, MapsActivity.class));
            }
        });
    }


    private void sosrun() {
        String number = sharedPreferences.getString("number","");
        if (!number.isEmpty())
        {
            if(ContextCompat.checkSelfPermission(HomeActivity.this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(HomeActivity.this, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(HomeActivity.this, SEND_SMS) == PackageManager.PERMISSION_GRANTED)
            {

                GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);
                String message = "Emergency SOS\nYou're receiving this message this contact has listed you as an emergency contact.\n" +
                        "https://maps.google.com/?q="+gpsTracker.getLatitude()+","+gpsTracker.getLongitude();
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(number,null,message,null,null);
                Snackbar snackbar = Snackbar
                        .make(MainLayout, "Message Sent Successfully!!!", Snackbar.LENGTH_LONG);
                snackbar.show();
            }

            if(ContextCompat.checkSelfPermission(HomeActivity.this, CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
            {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:"+number));
                startActivity(callIntent);
            }
        }

    }

    private void sosrunWear(double lat, double lon) {
        String number = sharedPreferences.getString("number","");
        if (!number.isEmpty())
        {
            if(ContextCompat.checkSelfPermission(HomeActivity.this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(HomeActivity.this, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(HomeActivity.this, SEND_SMS) == PackageManager.PERMISSION_GRANTED)
            {
                String message = "Emergency SOS\nYou're receiving this message this contact has listed you as an emergency contact.\n" +
                        "https://maps.google.com/?q="+lat+","+lon;
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(number,null,message,null,null);
                Snackbar snackbar = Snackbar
                        .make(MainLayout, "Message Sent Successfully!!!", Snackbar.LENGTH_LONG);
                snackbar.show();
            }

            if(ContextCompat.checkSelfPermission(HomeActivity.this, CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
            {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:"+number));
                startActivity(callIntent);
            }
        }

    }


    public boolean foregroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if(BgService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    //on resuming activity, reconnect play services
    public void onResume(){
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
        googleClient.connect();



        IsConnected();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent startIntent = new Intent(HomeActivity.this, BgService.class);
        startIntent.setAction("stop");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            if (foregroundServiceRunning())
                startForegroundService(startIntent);
        }
        else
        {
            if (foregroundServiceRunning())
                startService(startIntent);
        }
    }

    //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
        googleClient.disconnect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent startIntent = new Intent(HomeActivity.this, BgService.class);
        startIntent.setAction("start");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            if (!foregroundServiceRunning())
                startForegroundService(startIntent);
        }
        else
        {
            if (!foregroundServiceRunning())
                startService(startIntent);
        }
    }

    //watches for data item
    public void onDataChanged(DataEventBuffer dataEvents) {

        Log.d("Tag","changed");
        for(DataEvent event: dataEvents){

            //data item changed
            if(event.getType() == DataEvent.TYPE_CHANGED){

                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);

                Log.d("item",""+dataMapItem.getDataMap());

                if(item.getUri().getPath().equals("/Haenyeo_Health")){
                    double lat = dataMapItem.getDataMap().getDouble("lat");
                    double lon = dataMapItem.getDataMap().getDouble("lon");
                    HeartData = dataMapItem.getDataMap().getString("HeartRate");

                    int heartrate = Integer.parseInt(HeartData);
                    if (heartrate>=40 && heartrate <= 220)
                    {
                        tvnoheart.setVisibility(View.GONE);
                        tvHeartRate.setVisibility(View.VISIBLE);
                        tvHeartRate.setText(HeartData+" bpm");
                        progressBar.setMax(220);
                        progressBar.setMin(40);
                        progressBar.setProgress(Integer.parseInt(HeartData));
                        //SendHeartRateServer(Integer.parseInt(HeartData));
                    }
                }
                else
                {
                    tvnoheart.setVisibility(View.VISIBLE);
                    tvHeartRate.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
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
                                Wearable.getMessageClient(HomeActivity.this).sendMessage(node.getId(), datapath, message.getBytes());

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

}