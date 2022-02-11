package com.example.wearosv1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Calendar;

public class HomeActivity extends AppCompatActivity implements
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    String HeartData;
    private TextView tvHeartRate, tvwishes,tvnoheart;
    private ProgressBar progressBar;
    private GoogleApiClient googleClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        tvHeartRate = (TextView) findViewById(R.id.home_heart_rate_text);
        tvwishes = findViewById(R.id.home_heart_wishes);
        tvnoheart = findViewById(R.id.home_heart_rate_no_text);
        progressBar = findViewById(R.id.home_heart_rate_progress);

        //data layer
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

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

    }


    //on successful connection to play services, add data listner
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(googleClient, this);
        Log.d("Tag",""+googleClient.isConnected());
    }

    //on resuming activity, reconnect play services
    public void onResume(){
        super.onResume();
        googleClient.connect();
    }

    //on suspended connection, remove play services
    public void onConnectionSuspended(int cause) {
        Wearable.DataApi.removeListener(googleClient, this);
        Log.d("Tag","Suspend"+cause);
    }

    //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }

    //On failed connection to play services, remove the data listener
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("Tag","error :"+result.getErrorMessage());
        Wearable.DataApi.removeListener(googleClient, this);
    }

    //watches for data item
    public void onDataChanged(DataEventBuffer dataEvents) {

        Log.d("Tag","changed");
        for(DataEvent event: dataEvents){

            //data item changed
            if(event.getType() == DataEvent.TYPE_CHANGED){

                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);

                if(item.getUri().getPath().equals("/wearosheart")){
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



    //`isOnline`,`setUpAPIInformation` methods along with the `APIAsyncTask` class to go in here
}