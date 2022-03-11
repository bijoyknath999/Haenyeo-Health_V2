package com.HHMS;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.HHMS.models.Result;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import kotlin.ResultKt;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends WearableActivity
        implements SensorEventListener, DataClient.OnDataChangedListener, LocationListener {

    TextView TextHearRate;
    ImageView ImgHeart;
    private ObjectAnimator animator;
    private SensorManager sensorService;
    private Sensor heartSensor;
    private LinearLayout HeartRateClick, LocationClick;
    public static int i = 0;
    private LinearLayout SOS;

    String datapath = "/message_path";
    String message = "0";
    boolean check = false;

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

    private ImageButton Add, Remove;
    private TextView TimerText;
    private int timeval = 0;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private CountDownTimer cdt;
    private int finalheartrate;
    private RequestChecker requestChecker;
    private Button CheckSSAID;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        TextHearRate = findViewById(R.id.text_heart_rate);
        ImgHeart = findViewById(R.id.image_heart);
        HeartRateClick = findViewById(R.id.home_heart_click);
        SOS = findViewById(R.id.home_sos);
        LocationClick = findViewById(R.id.home_check_location);
        Add = findViewById(R.id.home_timer_add);
        Remove = findViewById(R.id.home_timer_remove);
        TimerText = findViewById(R.id.home_timer_text);
        CheckSSAID = findViewById(R.id.home_ssaid);



        requestChecker = new RequestChecker(HomeActivity.this);


        HeartRateClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestChecker.CheckingPermissionIsEnabledOrNot()) {
                    getHeartRate();
                }
                else
                    requestChecker.RequestMultiplePermission();
            }
        });

        SOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendSOS();
            }
        });

        LocationClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendLocationServer();
            }
        });

        sharedPreferences = getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        timeval = sharedPreferences.getInt("time",0);
        TimerText.setText(""+timeval+" min");



        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        Add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (timeval<5)
                {
                    timeval++;
                    TimerText.setText(""+timeval+" min");
                    editor.putInt("time",timeval);
                    editor.commit();
                    RunTimer();
                }
            }
        });

        Remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (timeval>1)
                {
                    timeval--;
                    TimerText.setText(""+timeval+" min");
                    editor.putInt("time",timeval);
                    editor.commit();
                    RunTimer();
                }
                else if (timeval==1)
                {
                    timeval--;
                    TimerText.setText(""+timeval+" min");
                    editor.putInt("time",timeval);
                    editor.commit();
                    if (cdt!=null)
                        cdt.cancel();
                }
            }
        });


        CheckSSAID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String androidId = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                Toast.makeText(HomeActivity.this, "SSAID : "+androidId, Toast.LENGTH_LONG).show();
            }
        });


        if (timeval>0)
            RunTimer();

    }

    private void RunTimer() {
        int time = sharedPreferences.getInt("time",0);
        int finaltime = time*60000;
        cdt = new CountDownTimer(finaltime,1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                if (requestChecker.CheckingPermissionIsEnabledOrNot())
                    getHeartRate();
                else
                    requestChecker.RequestMultiplePermission();
                SendHeartRateServer();
                SendLocationServerAuto();
                cdt.start();
            }
        };

        cdt.start();
    }

    private void SendLocationServerAuto() {
        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date();

        JSONObject data = new JSONObject();

        JSONObject jobj = new JSONObject();
        try {
            data.put("EQ_ID",""+androidId);
            data.put("LAT", ""+latitude);
            data.put("LNG", ""+longitude);
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
                        Log.v("Testing","Location Sent");
                    }
                }
                else
                {

                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cdt!=null)
            cdt.cancel();
    }

    private void SendHeartRateServer()
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
            data.put("HR_MAX", ""+finalheartrate);
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
                    }
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Log.d("Error ",""+t.getMessage());
            }
        });
    }

    private void SendSOS() {

        GetNode();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (check)
                    Toast.makeText(HomeActivity.this, "SOS Successful!!", Toast.LENGTH_SHORT).show();
                else
                    SendSOSServer();
            }
        }, 1000);
    }

    /*void SendSosData(boolean sos)
    {
        DataClient dataclient = Wearable.getDataClient(getApplicationContext());
        GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);
        double lat = gpsTracker.getLatitude();
        double lon = gpsTracker.getLongitude();
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/Haenyeo_Health");
        putDataMapReq.getDataMap().putString("HeartRate", "0");
        putDataMapReq.getDataMap().putDouble("lat", lat);
        putDataMapReq.getDataMap().putDouble("lon", lon);
        putDataMapReq.getDataMap().putBoolean("sos", sos);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = dataclient.putDataItem(putDataReq);

        putDataTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                Toast.makeText(HomeActivity.this, " Connected", Toast.LENGTH_SHORT).show();
            }
        });



    }*/


    void sendData(String heartrate) {

        DataClient dataclient = Wearable.getDataClient(getApplicationContext());

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/Haenyeo_Health");
        putDataMapReq.getDataMap().putString("HeartRate", heartrate);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = dataclient.putDataItem(putDataReq);

    }

    void getHeartRate() {
        Toast.makeText(getApplicationContext(), "Checking......", Toast.LENGTH_LONG).show();
        sensorService = (SensorManager) getSystemService(SENSOR_SERVICE);
        heartSensor = sensorService.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorService.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);

        animator = ObjectAnimator.ofPropertyValuesHolder(ImgHeart, PropertyValuesHolder.ofFloat("scaleX", 0.8F),
                PropertyValuesHolder.ofFloat("scaleY", 0.8f));
        animator.setDuration(300);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            int heart_rate = (int) event.values[0];
            if (heart_rate>=40 && heart_rate<=220)
            {
                animator.setDuration(60000 / heart_rate);
                animator.start();
                TextHearRate.setText(Integer.toString(heart_rate) +"");
                finalheartrate = heart_rate;
                sendData(String.valueOf(finalheartrate));
            }
        }
        else {
            // Do nothing
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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
        //Wearable.getDataClient(this).addListener(this);
        getLOcation();
        GetNode();
        /*Intent startIntent = new Intent(HomeActivity.this, BgService.class);
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
        }*/
    }


    //pause listener, disconnect play services
    public void onPause(){
        super.onPause();

        /*Wearable.getDataClient(this).removeListener(this);
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
        }*/
    }

    //function triggered every time there's a data change event
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event: dataEvents){

            //data item changed
            if(event.getType() == DataEvent.TYPE_CHANGED){

                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);

                //RESPONSE back from mobile message
                if(item.getUri().getPath().equals("/responsemessage")){

                }
            }
        }
    }



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
            data.put("LAT", ""+latitude);
            data.put("LNG", ""+longitude);
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
                        Log.v("Testing","SOS Sent");
                        Toast.makeText(HomeActivity.this, "SOS Send Successfully!!", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Error :"+response.body().getData().getMsg(), Toast.LENGTH_SHORT).show();
                        Log.d("Testing ",""+response.body().getData().getMsg());

                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "SOS Signal failed send!!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error :"+t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("Testing ",""+t.getMessage());
            }
        });
    }

    private void SendLocationServer()
    {

        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date();

        JSONObject data = new JSONObject();

        JSONObject jobj = new JSONObject();
        try {
            data.put("EQ_ID",""+androidId);
            data.put("LAT", ""+latitude);
            data.put("LNG", ""+longitude);
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
                        Log.v("Testing","Location Sent");
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


    private void GetNode()
    {
        check = false;
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

                        Task<Integer> sendMessageTask = null;
                        if (latitude!=0)
                        {
                            message = latitude+"_"+longitude;
                            sendMessageTask =
                                    Wearable.getMessageClient(HomeActivity.this).sendMessage(node.getId(),
                                            datapath, message.getBytes());
                        }
                        else
                        {
                            message = "0";
                            sendMessageTask =
                                    Wearable.getMessageClient(HomeActivity.this).sendMessage(node.getId(),
                                            datapath, message.getBytes());
                        }


                        try {
                            // Block on a task and get the result synchronously (because this is on a background
                            // thread).
                            Integer result = Tasks.await(sendMessageTask);
                            Log.v("Testing", "SendThread: message send to " + node.getDisplayName());

                        } catch (ExecutionException exception) {
                            Log.e("Testing", "Task failed2: " + exception);

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

    private void getLOcation()
        {

            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            }
            else
            {
                if (isNetworkEnabled) {
                    //check the network permission
                    if (ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) HomeActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                    }
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                if (isGPSEnabled){
                    if (location == null) {
                        //check the network permission
                        if (ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions((Activity) HomeActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                        }
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }

            }
        }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }



    //setup a broadcast receiver to receive the messages from the wear device via the listenerService.
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if(message.equals("0"))
                check = true;
        }
    }
}