package com.example.android.sunshine.app.sync;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by hardikarora on 2/15/16.
 */
public class SunshineWatchFaceService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event : dataEvents){
            if(event.getType() == DataEvent.TYPE_CHANGED){
                if(event.getDataItem().getUri().getPath().equals("/weather")){
                    SunshineSyncAdapter.syncImmediately(this);
                }
            }
        }
        super.onDataChanged(dataEvents);
    }
}
