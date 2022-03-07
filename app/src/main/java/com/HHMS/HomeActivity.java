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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

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
    private SharedPreferences.Editor editor;
    private RequestChecker requestChecker;
    private GoogleApiClient googleClient;
    private static final String FIND_ME_CAPABILITY_NAME = "find_me";





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

        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

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
                    Log.d("SOS ",""+response.body());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("Error ",""+t.getMessage());
            }
        });
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

    private void SendLocationServer()
    {
        GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);
        JSONObject data = new JSONObject();

        JSONObject LocData = new JSONObject();
        try {
            data.put("IMEI", "");
            data.put("LAT", gpsTracker.getLatitude());
            data.put("LNG", gpsTracker.getLongitude());

            LocData.put("ID", "LP");
            LocData.put("DATA", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ApiInterface.getRequestApiInterface().sendData(LocData.toString()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null)
                {
                    startActivity(new Intent(HomeActivity.this, MapsActivity.class));
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("Testing3",""+t.getMessage());
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
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(number,null,"Emergency SOS" +
                        "https://maps.google.com/?q="+gpsTracker.getLatitude()+","+gpsTracker.getLongitude(),null,null);
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
                GpsTracker gpsTracker = new GpsTracker(HomeActivity.this);
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(number,null,"Emergency SOS\n" +
                        "https://maps.google.com/?q="+lat+","+lon,null,null);
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

        IsConnected();

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
                    boolean sos = dataMapItem.getDataMap().getBoolean("sos");
                    Log.d("Tag","->lat :"+lat+", lon :"+lon);
                    HeartData = dataMapItem.getDataMap().getString("HeartRate");
                    Log.d("Tag",""+HeartData);

                    int heartrate = Integer.parseInt(HeartData);
                    if (heartrate>=40 && heartrate <= 220)
                    {
                        tvnoheart.setVisibility(View.GONE);
                        tvHeartRate.setVisibility(View.VISIBLE);
                        tvHeartRate.setText(HeartData+" bpm");
                        progressBar.setMax(220);
                        progressBar.setMin(40);
                        progressBar.setProgress(Integer.parseInt(HeartData));
                        SendHeartRateServer(Integer.parseInt(HeartData));
                    }

                    if (sos)
                    {
                        Log.d("Tag","SOS");
                        sosrunWear(lat,lon);
                        SendSOSServer();
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
}