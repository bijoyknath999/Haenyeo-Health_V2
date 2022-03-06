package com.HHMS;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends WearableActivity
        implements SensorEventListener, DataClient.OnDataChangedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Button button;
    private TextView mTextView;
    TextView TextHearRate;
    ImageView ImgHeart;
    private ObjectAnimator animator;
    private SensorManager sensorService;
    private Sensor heartSensor;
    private LinearLayout HeartRateClick, LocationClick;
    public static int i = 0;
    private LinearLayout SOS;
    private GoogleApiClient googleClient;
    private boolean IsConnected;
    private static final String FIND_ME_CAPABILITY_NAME = "find_me";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        //data layer
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleClient.connect();
        /*IsConnected();*/


        TextHearRate = findViewById(R.id.text_heart_rate);
        ImgHeart = findViewById(R.id.image_heart);
        HeartRateClick = findViewById(R.id.home_heart_click);
        SOS = findViewById(R.id.home_sos);
        LocationClick = findViewById(R.id.home_check_location);

        RequestChecker requestChecker = new RequestChecker(HomeActivity.this);
        if (requestChecker.CheckingPermissionIsEnabledOrNot())
            getHeartRate();
        else
            requestChecker.RequestMultiplePermission();

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
                if (IsConnected)
                    SendSosData(true);
                else
                    SendSOSServer();
            }
        });

        LocationClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, MapsActivity.class));
            }
        });


        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Toast.makeText(getApplicationContext(), "ID :"+androidId, Toast.LENGTH_SHORT).show();
    }


    private void IsConnected(){
        Wearable.NodeApi.getConnectedNodes(googleClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                List<Node> connectedNodes =
                        getConnectedNodesResult.getNodes();
                Log.d("Tag","node"+connectedNodes.size());
                Toast.makeText(getApplicationContext(), "Total Nodes : "+connectedNodes.size(), Toast.LENGTH_SHORT).show();
                if (connectedNodes.size()>0) {
                    IsConnected = true;
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                }
                else {
                    IsConnected = false;
                    Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void setOrUpdateNotification() {
        Wearable.CapabilityApi.getCapability(
                googleClient, FIND_ME_CAPABILITY_NAME,
                CapabilityApi.FILTER_REACHABLE).setResultCallback(
                new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult result) {
                        if (result.getStatus().isSuccess()) {
                            updateFindMeCapability(result.getCapability());
                        } else {
                            Log.e("Tag",
                                    "setOrUpdateNotification() Failed to get capabilities, "
                                            + "status: "
                                            + result.getStatus().getStatusMessage());
                        }
                    }
                });
    }

    private void updateFindMeCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        Log.d("Tag",""+connectedNodes);
        if (connectedNodes.isEmpty()) {
            IsConnected = false;
        } else {
            for (Node node : connectedNodes) {
                // we are only considering those nodes that are directly connected
                if (node.isNearby()) {
                   IsConnected = true;
                }
            }
        }
    }

    //on successful connection to play services, add data listner
    public void onConnected(Bundle connectionHint) {
        Wearable.getDataClient(this).addListener(this);
        Log.d("Tag",""+googleClient.isConnected());
        setOrUpdateNotification();
/*
        IsConnected();
*/
    }


    //on suspended connection, remove play services
    public void onConnectionSuspended(int cause) {
        Wearable.getDataClient(this).removeListener(this);
/*
        IsConnected();
*/
        Log.d("Tag","Suspend"+cause);
    }

    //On failed connection to play services, remove the data listener
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("Tag","error :"+result.getErrorMessage());
        Wearable.getDataClient(this).removeListener(this);
/*
        IsConnected();
*/
    }

    void SendSosData(boolean sos)
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

    }

    void sendData(String heartrate) {

        DataClient dataclient = Wearable.getDataClient(getApplicationContext());

        GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);
        double lat = gpsTracker.getLatitude();
        double lon = gpsTracker.getLongitude();
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/Haenyeo_Health");
        putDataMapReq.getDataMap().putString("HeartRate", heartrate);
        putDataMapReq.getDataMap().putDouble("lat", lat);
        putDataMapReq.getDataMap().putDouble("lon", lon);
        putDataMapReq.getDataMap().putBoolean("sos", false);
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
                sendData(Integer.toString(heart_rate));
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
        Wearable.getDataClient(this).addListener(this);
        googleClient.connect();

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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_STEM_1) {
                Toast.makeText(this, "one", Toast.LENGTH_SHORT).show();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_STEM_2) {
                Toast.makeText(this, "two", Toast.LENGTH_SHORT).show();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_STEM_3) {
                Toast.makeText(this, "three", Toast.LENGTH_SHORT).show();
                return true;
            }
            else if (keyCode == KeyEvent.KEYCODE_POWER)
            {
                Toast.makeText(this, "power", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "button"+keyCode, Toast.LENGTH_SHORT).show();
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    private void SendSOSServer()
    {
        Log.d("Testing ","Testing");

        JSONObject data = new JSONObject();

        JSONObject jobj = new JSONObject();
        try {
            data.put("IMEI","");
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
                    Toast.makeText(getApplicationContext(), "SOS Signal send successfully!!", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "SOS Signal failed send!!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error :"+t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("Testing ",""+t.getMessage());
            }
        });
    }
}