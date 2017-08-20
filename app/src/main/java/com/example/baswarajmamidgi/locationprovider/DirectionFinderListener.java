package com.example.baswarajmamidgi.locationprovider;

import java.util.List;



public interface DirectionFinderListener {
    void onDirectionFinderStart();
    void onDirectionFinderSuccess(List<Route> route);
}
