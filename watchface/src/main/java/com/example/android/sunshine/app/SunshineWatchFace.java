/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {

    private static final String  LOG_TAG = SunshineWatchFace.class.getSimpleName();

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    public final String WEATHER_PATH  = "/weather";
    public final String WEATHER_DETAILS_PATH  = "/weatherDetails";
    private final String MAX_TEMP_KEY = "maxTemp";
    private final String MIN_TEMP_KEY= "minTemp";
    private final String WEATHER_KEY = "weatherId";


    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;


        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }




    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mDateTextPaint;
        Paint temperaturePaint;
        boolean mAmbient;
        Time mTime;

        // Climate details
        String lowTemperature;
        String highTemperature;
        int weatherIconId;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(SunshineWatchFace.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        float mXOffset, mYOffset;

        float centerX, centerY;

        float mXDateOffset, mYDateOffset;

        float mXTimeOffset, mYTimeOffset;

        float mXWeatherOffset, mYWeatherOffset;


        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            Log.d(LOG_TAG, "The data has been changed");
            // Update the data on the watch
            for(DataEvent event : dataEventBuffer){
                DataItem dataItem = event.getDataItem();
                String path = dataItem.getUri().getPath();
                Log.d(LOG_TAG, "Path : " + path);
                if(path.compareTo(WEATHER_DETAILS_PATH) == 0){
                    Log.d(LOG_TAG, "Got the weather details");
                    setWeatherData(dataItem);
                }
            }

            invalidate();
        }

        private void setWeatherData(DataItem dataItem){
            DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
            highTemperature = dataMap.getString(MAX_TEMP_KEY, null);
            lowTemperature = dataMap.getString(MIN_TEMP_KEY, null);
            int weatherId = dataMap.getInt(WEATHER_KEY, -1);
            Log.d(LOG_TAG, "High Temperature : " + highTemperature);
            Log.d(LOG_TAG, "Low Temperature : " + lowTemperature);
            Log.d(LOG_TAG, "weatherId: " + weatherId);
            if(weatherId != -1){
                weatherIconId = Utility.getIconResourceForWeatherCondition(weatherId);
            }
        }





        @Override
        public void onConnected(Bundle bundle) {
            // Request weather info
            Log.d(LOG_TAG, "Watch face has been connected");
            Wearable.DataApi.addListener(googleApiClient, Engine.this);
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEATHER_PATH);
            dataMapRequest.getDataMap().putString("uuid_code", UUID.randomUUID().toString());
            PutDataRequest dataRequest = dataMapRequest.asPutDataRequest();

            Wearable.DataApi.putDataItem(googleApiClient, dataRequest)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d(LOG_TAG, "Asked for weather data : " +
                                dataItemResult.getStatus().toString());
                    }
                });

        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(LOG_TAG, "COnnection has been suspended");
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e(LOG_TAG, "COnnection has been failed");
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.primary));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.primary_light));

            mDateTextPaint = new Paint();
            mDateTextPaint = createTextPaint(resources.getColor(R.color.primary_light));

            temperaturePaint = new Paint();
            temperaturePaint = createTextPaint(resources.getColor(R.color.primary_light));

            mTime = new Time();


        }


        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }



        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                // Connect to the api, when the watch screen is visible.
                googleApiClient.connect();

                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();

                if (googleApiClient != null && googleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(googleApiClient, this);
                    googleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();


            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);

            mXDateOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_date_offset_round : R.dimen.digital_x_date_offset);
            mYDateOffset = resources.getDimension(isRound
                    ? R.dimen.digital_date_y_offset_round : R.dimen.digital_date_y_offset);

            mXTimeOffset = resources.getDimension(isRound
                    ? R.dimen.digital_time_x_offset_round : R.dimen.digital_time_x_offset);
            mYTimeOffset = resources.getDimension(isRound
                    ? R.dimen.digital_time_y_offset_round : R.dimen.digital_time_y_offset);

            mXWeatherOffset = resources.getDimension(isRound
                    ? R.dimen.digital_weather_x_offset_round : R.dimen.digital_weather_x_offset);
            mYWeatherOffset = resources.getDimension(isRound
                    ? R.dimen.digital_weather_y_offset_round : R.dimen.digital_weather_y_offset);


            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float dateTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_date_text_size_round : R.dimen.digital_date_text_size);
            float temperatureTextSize = resources.getDimension(isRound
                    ? R.dimen.temperature_text_size_round : R.dimen.temperature_text_size);

            mTextPaint.setTextSize(textSize);
            mDateTextPaint.setTextSize(dateTextSize);
            temperaturePaint.setTextSize(temperatureTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            invalidate();
            centerX = bounds.centerX();
            centerY = bounds.centerY();

            // Draw the background.
            if (mAmbient){
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String text = String.format("%d:%02d", mTime.hour, mTime.minute);
            Rect textBounds = new Rect();
            mTextPaint.getTextBounds(text, 0, text.length(), textBounds);

            mXTimeOffset = centerX - (textBounds.width() / 2f);
            mYTimeOffset = (centerY / 2f);
            canvas.drawText(text, mXTimeOffset, mYTimeOffset, mTextPaint);

            String dateStr = String.format(getDay(mTime.weekDay)
                    + ", " + getMonth(mTime.month) + " " +
                    mTime.year);

            mDateTextPaint.getTextBounds(dateStr, 0, dateStr.length(), textBounds);

            mXDateOffset = centerX - (textBounds.width() / 2f);
            mYDateOffset = centerY - (1.5f * textBounds.height());

            canvas.drawText(dateStr, mXDateOffset, mYDateOffset, mDateTextPaint);

            canvas.drawLine(centerX - 20, centerY, centerX + 20,
                    centerY, mTextPaint);
            if(!mAmbient) {
                drawClimateDetails(canvas, textBounds);
            }

        }


        private void drawClimateDetails(Canvas canvas, Rect bounds){
            mXWeatherOffset = centerX - (centerX / 2f);
            mYWeatherOffset = centerY + 15;
            if(weatherIconId != -1){
                try {
                    Drawable drawable = getResources().getDrawable(weatherIconId);
                    Bitmap icon = ((BitmapDrawable) drawable).getBitmap();
                    int size = (int) mDateTextPaint.getTextSize() * 2;
                    Bitmap weatherIcon = Bitmap.createScaledBitmap(icon, size,
                            size, true);
                    if (!isInAmbientMode()) {
                        // If it is not in ambient mode, show
                        // the weather icon.
                        canvas.drawBitmap(weatherIcon, mXWeatherOffset, mYWeatherOffset, null);
                    }
                    mXWeatherOffset += weatherIcon.getWidth() + 10;
                } catch(Resources.NotFoundException e){


                }

            }
            if(highTemperature != null) {
                canvas.drawText(highTemperature, mXWeatherOffset, mYWeatherOffset + 30,
                        mDateTextPaint);
                temperaturePaint.getTextBounds(highTemperature, 0, highTemperature.length(), bounds);
                mXWeatherOffset += bounds.right + 30;
            }
            if(lowTemperature != null) {
                canvas.drawText(lowTemperature, mXWeatherOffset, mYWeatherOffset + 30,
                        mDateTextPaint);
            }
        }


        private String getDay(int day){
            DateFormatSymbols dfs = new DateFormatSymbols();
            String[] weekdays = dfs.getWeekdays();
            String weekSymbol = weekdays[day];
            return weekSymbol.substring(0, 3).toUpperCase();
        }

        private String getMonth(int month){
            DateFormatSymbols dfs = new DateFormatSymbols();
            String[] months = dfs.getMonths();
            String monthSymbol = months[month];
            return monthSymbol.substring(0, 3).toUpperCase();
        }



        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
