package com.broto.geoalarm;

import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapHelper {

    private static final String TAG = "MapHelper";

    private LatLng target;
    private int radius;
    private static GoogleMap mMap;
    private static MapHelper MapHelperInstance;

    private MapHelper(){

    }

    public static MapHelper getInstance(GoogleMap map){
        Log.i(TAG,"getInstance");
        if(MapHelperInstance == null)
            MapHelperInstance = new MapHelper();
        mMap = map;
        return MapHelperInstance;
    }

    //This method show olny be called initially when onMapReady callback is received
    public void initMapUI(){
        Log.i(TAG,"initMapUI()");
        //TODO:: Location HardCoded : Bansdroni
        target = new LatLng(22.4696, 88.3631);
        radius = Constants.defaultRadius;
        //TODO:HardCoded String and Location
        MapAddMarker(target,"Bansdroni",true);
        MapAddCircle(radius);
    }

    //This method will be called everytime the user selects a new target
    public void updateTarget(LatLng latLng,int rad){
        Log.i(TAG,"updateTarget:: Target: " + latLng + " Radius: " + rad);
        target = latLng;
        radius = rad;

        mMap.clear();

        MapAddMarker(target,"Target",false);
        MapAddCircle(radius);
    }

    public void updateLiveLocation(LatLng live){
        Log.i(TAG,"updateLiveLocation");
        mMap.clear();
        MapAddMarker(target,"Target",false);
        MapAddCircle(radius);

        //TODO:: try enabling the zoom
        MapAddMarker(live,"Live",false);
    }

    public void removeLiveMarker(){
        Log.i(TAG,"removeLiveMarker");
        mMap.clear();
        MapAddMarker(target,"Target",false);
        MapAddCircle(radius);
    }

    public boolean enableTargetAdd(GoogleMap.OnMapLongClickListener listener){
        Log.i(TAG,"enableTargetAdd");
        mMap.setOnMapLongClickListener(listener);
        return true;
    }

    public boolean disableTargetAdd(){
        Log.i(TAG,"disableTargetAdd");
        mMap.setOnMapLongClickListener(null);
        return true;
    }

    private void MapAddMarker(LatLng latLng,String title,boolean zoom_flag){
        Log.i(TAG,"MapAddMarker target: " + latLng +
                " title: " + title +
                " zoom_flag: " + zoom_flag);
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
            Log.i(TAG,"Map marker sucessfully set at Lat:"+latLng.latitude+
                    " Long:"+latLng.longitude+" Title:"+title);
        }
        catch (Exception e){
            Log.e(TAG,"Error setting marker at Lat:"+latLng.latitude+
                    " Long:"+latLng.longitude+" Title:"+title);
            e.printStackTrace();
        }
    }

    private void MapAddCircle(int rad){
        Log.i(TAG,"MapAddCircle Radius: " + rad);
        try{
            mMap.addCircle(new CircleOptions()
                    .center(target)
                    .radius(rad)
                    .strokeWidth(0f)
                    .fillColor(0x55c5e26d)
            );
            Log.i(TAG,"Set up radius, val="+rad);
        }
        catch (Exception e){
            Log.e(TAG,"Error setting up radius, val="+rad);
            e.printStackTrace();
        }
    }

}
