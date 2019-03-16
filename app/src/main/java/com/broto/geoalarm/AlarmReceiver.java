package com.broto.geoalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class AlarmReceiver extends BroadcastReceiver {

    private final String TAG = "AlarmReceiver";
    private ArrayList<IAlarmCallback> mAlarmCallback = new ArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG,"Received Intent : " + intent.getAction());
        if (intent.getAction() == Constants.ALARM_INTENT){
            showNotification(context);
            alertClients();
        }
        else {
            Log.i(TAG,"Invalid Intent. Ignore");
        }
    }

    public void registerAlarmTriggerCallback(IAlarmCallback callback){
        mAlarmCallback.add(callback);
    }

    private void alertClients(){
        for(IAlarmCallback callback : mAlarmCallback)
            callback.onAlarmTrigger();
    }

    private void showNotification(Context context){
        Log.i(TAG,"Entered Location");
        Toast.makeText(context, "Alarm", Toast.LENGTH_SHORT).show();
    }
}
