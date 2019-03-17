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
import android.provider.Settings;
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
    private GoogleMap.OnMapLongClickListener mMapLongClickListener;
    private LocationListener locationListener;
    private LocationController mLocationController;
    private AlarmReceiver mAlarmReceiver;
    private MapHelper mMapHelper;

    private LatLng target;
    private int radius;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mMap  = null;

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setUpAlarmReceiver();
        initializeUI();

        initLocationListener();
        initMapLongClickListener();
    }

    @Override
    protected void onResume() {
        Log.i(TAG,"onResume()");
        mLocationController = LocationController.getInstance(getApplicationContext());
        super.onResume();
        if(mMap != null){
            mMapHelper.enableTargetAdd(mMapLongClickListener);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG,"onPause()");
        if(mMap != null) {
            mMapHelper.disableTargetAdd();
        }
        super.onPause();
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
        Log.i(TAG, "GoogleMap Ready.");
        mMap = googleMap;

        setAlarmButton.setEnabled(true);
        radiusBar.setEnabled(true);

        mMapHelper = MapHelper.getInstance(mMap);

        target = new LatLng(22.4696, 88.3631);
        radius = Constants.defaultRadius;

        mMapHelper.initMapUI();
        setSeekBar(radius);

        mMapHelper.enableTargetAdd(mMapLongClickListener);
    }

    private void initializeUI(){

        Log.i(TAG,"initializeUI()");

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

                int result = mLocationController.registerLiveLocationUpdate(locationListener);
                switch(result){
                    case Constants.LOCATION_PERMISSION_UNAVAILABLE:
                        checkPermission();
                        return;
                    case Constants.LOCATION_PROVIDER_DISABLED:
                        enableLocationProvider();
                        return;
                    case Constants.LOCATION_ENABLED:
                        break;
                    default:
                        Log.e(TAG,"Unexpected registerLiveLocationUpdate result: " + result);
                        return;
                }

                registerReceiver(mAlarmReceiver,new IntentFilter(Constants.ALARM_INTENT));

                result = mLocationController.setAlarm(target,radius);
                switch(result){
                    case Constants.LOCATION_PERMISSION_UNAVAILABLE:
                        checkPermission();
                        return;
                    case Constants.LOCATION_PROVIDER_DISABLED:
                        enableLocationProvider();
                        return;
                    case Constants.LOATION_ALARM_ALREADY_ENABLED:
                        handleCurrentAlarm();
                        return;
                    case Constants.LOCATION_ALARM_SUCCESS:
                        break;
                    default:
                        Log.e(TAG,"Unexpected setAlarm result: " + result);
                        return;
                }

                mMapHelper.disableTargetAdd();

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
                mMapHelper.removeLiveMarker();
                mMapHelper.enableTargetAdd(mMapLongClickListener);

                try{
                    unregisterReceiver(mAlarmReceiver);
                } catch(Exception e){
                    Log.e(TAG,"Alarm Receiver already unregistered");
                    e.printStackTrace();
                }

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
                mMapHelper.updateTarget(target,radius);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void initMapLongClickListener(){
        mMapLongClickListener = new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                target = latLng;
                radius = Constants.defaultRadius;

                mMapHelper.updateTarget(target,radius);
                setSeekBar(radius);
            }
        };
    }

    private void initLocationListener(){

        Log.i(TAG,"initLocationListener()");

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                Log.i(TAG, "Location Updated. " +
                        location.getLatitude() + " " +
                        location.getLongitude()
                );

                mMapHelper.updateLiveLocation(
                        new LatLng(location.getLatitude(),location.getLongitude()));
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
    }

    private void setUpAlarmReceiver() {
        Log.i(TAG,"setUpAlarmReceiver()");
        mAlarmReceiver = new AlarmReceiver();
        mAlarmReceiver.registerAlarmTriggerCallback(new IAlarmCallback() {
            @Override
            public void onAlarmTrigger() {
                Log.i(TAG,"onAlarmTrigger Callback");
                //TODO:: Improve the Logic
                mLocationController.unregisterLiveLocationUpdate(locationListener);
                mMapHelper.removeLiveMarker();
                mMapHelper.enableTargetAdd(mMapLongClickListener);
                mLocationController.stopAlarm();
                try {
                    unregisterReceiver(mAlarmReceiver);
                } catch (Exception e) {
                    Log.e(TAG,"Alarm Receiver Already Unregistered");
                    e.printStackTrace();
                }

                //Update HMI State
                stopAlarmButton.setEnabled(false);
                radiusBar.setEnabled(true);
                setAlarmButton.setEnabled(true);
            }
        });
    }

    private void setSeekBar(int radius){
        Log.i(TAG,"setUpSeekBar() radius: " + radius);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            radiusBar.setProgress(radius/Constants.seekMultipler,true);
        }
        else{
            radiusBar.setProgress(radius/Constants.seekMultipler);
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

    private void enableLocationProvider(){
        Log.i(TAG,"enableLocationProvider()");
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void handleCurrentAlarm() {
        Log.i(TAG,"handleCurrentAlarm");
        //TODO:: Complete implementation
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
