package com.example.android.sunshine.app;

import android.util.Log;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by hardikarora on 2/15/16.
 */
public class SunshineWatchFaceService extends WearableListenerService {

    private static final String LOG_TAG = SunshineWatchFaceService.class.getSimpleName();

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "On Create : " + LOG_TAG);
        super.onCreate();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(LOG_TAG, "onMessageReceived : " + LOG_TAG);
        super.onMessageReceived(messageEvent);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        Log.d(LOG_TAG, "Data has been changed in the service.");

        for(DataEvent event : dataEvents){
//            if(event.getType() == DataEvent.TYPE_CHANGED){
                if(event.getDataItem().getUri().getPath().equals("/weather")){
                    SunshineSyncAdapter.syncImmediately(this);
//                }
            }
        }
        super.onDataChanged(dataEvents);
    }
}
