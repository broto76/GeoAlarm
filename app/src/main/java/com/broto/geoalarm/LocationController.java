package com.broto.geoalarm;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class LocationController{

    private static final String TAG = "LocationController";

    private static LocationController mInstance;

    private static Context mContext;
    private static LocationManager mLocationManager;
    private PendingIntent mPendingIntent;

    private LocationController(){
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    }

    public static LocationController getInstance(Context context){
        Log.i(TAG,"LocationController::getInstance");
        mContext = context;
        if(mInstance == null)
            mInstance = new LocationController();
        return mInstance;
    }

    /*    This method returns the following integer based on the result
          LOCATION_ENABLED = when it can successfully register for location updates
          LOCATION_PROVIDER_DISABLED = when location provider is unavailable
          LOCATION_PERMISSION_UNAVAILABLE = when location permission is unavailable
     */
    //IMPORTANT :: Make sure to check location permission before this method
    public int registerLiveLocationUpdate(LocationListener locationListener){
        Log.i(TAG,"LocationController::registerLiveLocationUpdate");
        int result = Constants.LOCATION_ENABLED;
        try {
            if(checkLocationProvierEnabled()) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                        0, locationListener);
                Log.i(TAG, "Successfully registered for Location Updates");
            }
            else{
                result = Constants.LOCATION_PROVIDER_DISABLED;
                Log.e(TAG,"Location Provider not enabled");
            }
        } catch (SecurityException e) {
            Log.e(TAG,"Security Exception. Location Permission unavailable");
            result = Constants.LOCATION_PERMISSION_UNAVAILABLE;
            e.printStackTrace();
        }
        return result;
    }

    public void unregisterLiveLocationUpdate(LocationListener locationListener){
        Log.i(TAG,"LocationController::unregisterLiveLocationUpdate");
        mLocationManager.removeUpdates(locationListener);
    }

    /*    This method returns the following integer based on the result
          LOCATION_ALARM_SUCCESS = when it can successfully register for location updates
          LOCATION_PROVIDER_DISABLED = when location provider is unavailable
          LOCATION_PERMISSION_UNAVAILABLE = when location permission is unavailable
          LOATION_ALARM_ALREADY_ENABLED = Alarm already set
    */
    //IMPORTANT :: Make sure to check location permission before this method
    public int setAlarm(LatLng location,float radius){
        Log.i(TAG,"LocationController::setAlarm");
        int result = Constants.LOCATION_ALARM_SUCCESS;

        if(mPendingIntent == null) {
            setUpPendingIntent();
        }
        else {
            Log.i(TAG,"Alarm already set.");
            return Constants.LOATION_ALARM_ALREADY_ENABLED;
        }

        try {
            if(checkLocationProvierEnabled()) {
                mLocationManager.addProximityAlert(location.latitude, location.longitude,
                        radius, -1,
                        mPendingIntent);
                Log.w(TAG, "Successfully registered for Location Alarm");
            }
            else{
                result = Constants.LOCATION_PROVIDER_DISABLED;
                Log.e(TAG,"Location Provider not enabled");
            }
        } catch (SecurityException e) {
            Log.e(TAG,"Security Exception. Location Permission unavailable");
            result = Constants.LOCATION_PERMISSION_UNAVAILABLE;
            e.printStackTrace();
        }
        return result;
    }

    public void stopAlarm(){
        Log.i(TAG,"LocationController::stopAlarm");
        if(mPendingIntent!=null){
            mLocationManager.removeProximityAlert(mPendingIntent);
            mPendingIntent = null;
        }
    }

    public boolean checkLocationProvierEnabled(){
        Log.i(TAG,"LocationController::checkLocationProvierEnabled");
        boolean result = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.i(TAG,"GPS Enabled = " + result);//IMPORTANT :: Make sure to check location permission before this method
        return result;
    }

    private void setUpPendingIntent(){
        Intent intent = new Intent(Constants.ALARM_INTENT);
        mPendingIntent = PendingIntent.getBroadcast(mContext,0,
                intent,PendingIntent.FLAG_CANCEL_CURRENT);
        Log.i(TAG,"setUpPendingIntent()");
    }
}
