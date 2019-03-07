package com.broto.geoalarm;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private String TAG = "MapActivity";
    private final int locationRequestCode = 1;

    private Button setAlarmButton;
    private Button stopAlarmButton;
    private SeekBar radiusBar;

    private GoogleMap mMap;
    private LocationListener locationListener;
    private LocationController mLocationController;

    private LatLng target;
    private int radius;

    private BroadcastReceiver alarmReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setAlarmButton = findViewById(R.id.SetAlarm);
        stopAlarmButton = findViewById(R.id.StopAlarm);
        radiusBar = findViewById(R.id.radiusBar);

        setAlarmButton.setEnabled(false);
        stopAlarmButton.setEnabled(false);
        radiusBar.setEnabled(false);

        setAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.i(TAG,"Set Alarm Button");

                mLocationController.registerLiveLocationUpdate(locationListener);

                mLocationController.setAlarm(target,radius);

                setAlarmButton.setEnabled(false);
                radiusBar.setEnabled(false);
                stopAlarmButton.setEnabled(true);
            }
        });

        stopAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.i(TAG,"Stop Alarm Button");

                mLocationController.unregisterLiveLocationUpdate(locationListener);

                mLocationController.stopAlarm();

                stopAlarmButton.setEnabled(false);
                radiusBar.setEnabled(true);
                setAlarmButton.setEnabled(true);
            }
        });

        radiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                radius = i*Constants.seekMultipler;
                mMap.clear();
                MapAddMarker(target,"Target",false);
                MapAddCircle(radius);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                mMap.clear();

                MapAddMarker(target,"Target",false);
                MapAddCircle(radius);
                setUpSeekBar(radius);

                MapAddMarker(new LatLng(location.getLatitude(),location.getLongitude()),
                        "Live",false);

                Log.i(TAG, "Location Updated. " +
                        location.getLatitude() + " " +
                        location.getLongitude()
                );
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        setUpAlarmReceiver();
    }

    @Override
    protected void onResume() {
        Log.i(TAG,"onResume()");
        mLocationController = LocationController.getInstance(getApplicationContext());
        super.onResume();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Bansdroni, India.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG + ":onMapReady", "GoogleMap Ready.");
        mMap = googleMap;

        setAlarmButton.setEnabled(true);
        radiusBar.setEnabled(true);

        //Setup initial Map Marker
        LatLng home = new LatLng(22.4696, 88.3631);
        target = home;
        radius = Constants.defaultRadius;
        //TODO:HardCoded String and Location
        MapAddMarker(target,"Bansdroni",true);
        MapAddCircle(radius);
        setUpSeekBar(radius);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                target = latLng;
                radius = Constants.defaultRadius;

                mMap.clear();

                MapAddMarker(target,"Target",false);
                MapAddCircle(radius);
                setUpSeekBar(radius);
            }
        });
    }

    private void setUpAlarmReceiver() {
        Log.i(TAG,"setUpAlarmReceiver()");
        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG,"Entered Location");
                stopAlarmButton.performClick();
                Toast.makeText(context, "Alarm", Toast.LENGTH_SHORT).show();
            }
        };
        registerReceiver(alarmReceiver,
                new IntentFilter(Constants.ALARM_INTENT));
    }

    private void setUpSeekBar(int radius){
        Log.i(TAG,"setUpSeekBar()");
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            radiusBar.setProgress(radius/Constants.seekMultipler,true);
        }
        else{
            radiusBar.setProgress(radius/Constants.seekMultipler);
        }
    }

    private void MapAddMarker(LatLng latLng,String title,boolean zoom_flag){
        try{
            // Add a marker in Bansdroni and move the camera
            mMap.addMarker(new MarkerOptions().position(latLng).title(title));

            //Configure CameraPosition
            //Zoom change based on flag
            if(zoom_flag){
                CameraPosition position = new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(Constants.defaultZoom)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
            }
            Log.i(TAG+":MapAddMarker","Map marker sucessfully set at Lat:"+latLng.latitude+
                    " Long:"+latLng.longitude+" Title:"+title);
        }
        catch (Exception e){
            Log.e(TAG+":MapAddMarker","Error setting marker at Lat:"+latLng.latitude+
                    " Long:"+latLng.longitude+" Title:"+title);
            e.printStackTrace();
        }
    }

    private void MapAddCircle(int rad){
        try{
            mMap.addCircle(new CircleOptions()
                    .center(target)
                    .radius(rad)
                    .strokeWidth(0f)
                    .fillColor(0x55c5e26d)
            );
            Log.i(TAG+":MapAddCircle","Set up radius, val="+rad);
        }
        catch (Exception e){
            Log.e(TAG+":MapAddCircle","Error setting up radius, val="+rad);
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG,"onDestroy()");
        if(mLocationController != null){
            mLocationController.stopAlarm();
            mLocationController.unregisterLiveLocationUpdate(locationListener);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG,"onBackPressed()");
        Toast.makeText(this, "Exiting", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }

    private boolean checkPermission(){
        Log.i(TAG,"checkPermission()");
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else{
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            },locationRequestCode);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode){
            case locationRequestCode:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Log.i(TAG,"Permission Granted");
                }
                else{
                    AlertDialog.Builder alertBuilder =
                            new AlertDialog.Builder(getApplicationContext());
                    alertBuilder.setTitle("Insufficient Permission")
                            .setMessage("Please Allow Location Permission")
                            .setPositiveButton("Enable Location Permission",
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    requestPermissions(new String[]{
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                    },locationRequestCode);
                                }
                            })
                            .setNegativeButton("Exit",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            finish();
                                        }
                                    })
                            .show();
                }
                break;
        }
    }
}
