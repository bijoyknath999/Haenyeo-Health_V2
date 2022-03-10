package com.HHMS;

import androidx.fragment.app.FragmentActivity;

import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private GpsTracker gpsTracker;
    private double latitude = 0.0,longitude = 0.0;
    private LatLng latLng;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        gpsTracker = new GpsTracker(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        //get user current location latitude and longitude
        if (gpsTracker!=null) {
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();
        }
        else
        {
            Toast.makeText(MapsActivity.this, "Failed to get your location, Please reopen.", Toast.LENGTH_SHORT).show();
        }

        //it will get user current location full address
        latLng = new LatLng(latitude,longitude);
        //it will set marker on user current location on google map
        mMap.addMarker(new MarkerOptions().position(latLng));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
    }
}