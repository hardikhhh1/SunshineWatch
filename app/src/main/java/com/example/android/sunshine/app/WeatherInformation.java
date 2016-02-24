package com.example.android.sunshine.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * Created by hardikarora on 2/10/16.
 */
public class WeatherInformation {

    private double maxTemp;
    private double minTemp;
    private String description;
    private int weatherArtResourceId;

    public WeatherInformation(double maxTemp, double minTemp, String description, int weatherArtResourceId) {
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.description = description;
        this.weatherArtResourceId = weatherArtResourceId;
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(double maxTemp) {
        this.maxTemp = maxTemp;
    }

    public double getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(double minTemp) {
        this.minTemp = minTemp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getWeatherArtResourceId() {
        return weatherArtResourceId;
    }

    public void setWeatherArtResourceId(int weatherArtResourceId) {
        this.weatherArtResourceId = weatherArtResourceId;
    }

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_SHORT_DESC = 1;
    private static final int INDEX_MAX_TEMP = 2;
    private static final int INDEX_MIN_TEMP = 3;

    public static WeatherInformation getWeatherInformation(String location, ContentResolver resolver){
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor data = resolver.query(weatherForLocationUri, FORECAST_COLUMNS, null,
                null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
        if (data == null) {
            return null;
        }
        if (!data.moveToFirst()) {
            data.close();
            return null;
        }

        // Extract the weather data from the Cursor
        int weatherId = data.getInt(INDEX_WEATHER_ID);
        int weatherArtResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
        String description = data.getString(INDEX_SHORT_DESC);
        double maxTemp = data.getDouble(INDEX_MAX_TEMP);
        double minTemp = data.getDouble(INDEX_MIN_TEMP);
        data.close();
        return new WeatherInformation(maxTemp, minTemp, description, weatherArtResourceId);
    }


}
