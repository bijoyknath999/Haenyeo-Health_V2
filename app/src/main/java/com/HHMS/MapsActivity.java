package com.HHMS;

import androidx.fragment.app.FragmentActivity;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.HHMS.databinding.ActivityMapsBinding;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Geocoder geo;
    private GpsTracker gpsTracker;
    private double latitude = 0.0,longitude = 0.0, latitude2= 0.0,longitude2 = 0.0;
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
        geo = new Geocoder(MapsActivity.this, Locale.getDefault());
        //get user current location latitude and longitude
        if (gpsTracker!=null) {
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();
        }

        try {
            //it will get user current location full address
            if (geo == null)
                geo = new Geocoder(MapsActivity.this, Locale.getDefault());
            List<Address> address = geo.getFromLocation(latitude,longitude, 1);
            latLng = new LatLng(latitude,longitude);
            //it will set marker on user current location on google map
            mMap.addMarker(new MarkerOptions().position(latLng).title(address.get(0).getAddressLine(0)));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        } catch (IOException ex) {
            if (ex != null)
                Toast.makeText(MapsActivity.this, "Error:" + ex.getMessage().toString(), Toast.LENGTH_LONG).show();
        }
    }
}