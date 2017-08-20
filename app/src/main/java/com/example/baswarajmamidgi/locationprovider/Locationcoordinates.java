package com.example.baswarajmamidgi.locationprovider;

/**
 * Created by baswarajmamidgi on 23/09/16.
 */
public class Locationcoordinates {
    private String latitude;
    private String longitude;


    public Locationcoordinates(String latitude, String longitude)
    {
        this.latitude=latitude;
        this.longitude=longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
}
