package com.mycompany.myfirstindoorsapp;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.customlbs.library.Indoors;
import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.LocalizationParameters;
import com.customlbs.library.callbacks.DebugInfoCallback;
import com.customlbs.library.callbacks.IndoorsServiceCallback;
import com.customlbs.library.model.Building;
import com.customlbs.library.model.DebugInfo;
import com.customlbs.library.model.Zone;
import com.customlbs.shared.Coordinate;

import com.mycompany.myfirstindoorsapp.ImageTargets.ImageTargets;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ine on 18/03/2015.
 */
public class LocationActivity extends FragmentActivity implements IndoorsServiceCallback,
        IndoorsLocationListener, DebugInfoCallback{

//    private TextView textView;
    private Indoors indoors;
    private List<Zone> zones;
    private ImageTargets imageTargets;

    public LocationActivity(ImageTargets imageTargets){
        zones = new ArrayList<Zone>();
        this.imageTargets = imageTargets;
        IndoorsFactory.createInstance(imageTargets, "d2b8119f-49b4-4e21-a67b-67fa90a17b45", this, false);
        Log.d("oncreate", "LocationActivity");
    }

    @Override
    public void connected() {
        indoors = IndoorsFactory.getInstance();
        Log.d("connected", "");
        indoors.registerLocationListener(this);
        indoors.setLocatedCloudBuilding((long) 281079350, new LocalizationParameters());
    }

    @Override
    public void onError(IndoorsException indoorsException) {
        Log.d("error listener", indoorsException.toString());
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("stopped", "");
        indoors.removeLocationListener(this);
        IndoorsFactory.releaseInstance(this);
    }

    @Override
    public void buildingLoaded(Building building) {
        Log.d("building loaded", building.toString());
    }

    @Override
    public void changedFloor(int floorLevel, String name) {
    }

    @Override
    public void enteredZones(List<Zone> zones) {
        if(zones.size() > 0){
            this.zones = zones;
            this.imageTargets.enteredZones(zones);
        }
    }

    @Override
    public void leftBuilding(Building building) {
        Log.d("left building", "");
    }

    @Override
    public void loadingBuilding(int progress) {
        Log.d("loading building", progress+"");
    }

    @Override
    public void orientationUpdated(float orientation) {
//        Log.d("orientation update", orientation+"");
    }

    @Override
    public void positionUpdated(Coordinate userPosition, int accuracy) {
        Log.d("located", userPosition.toString());
    }

    @Override
    public void setDebugInfo(DebugInfo debugInfo) {
        Log.d("DEBUGINFO", debugInfo.toString());
    }
}

