package com.mycompany.myfirstindoorsapp;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.customlbs.coordinates.GeoCoordinate;
import com.customlbs.library.Indoors;
import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.IndoorsLocationAdapter;
import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.LocalizationParameters;
import com.customlbs.library.callbacks.DebugInfoCallback;
import com.customlbs.library.callbacks.IndoorsServiceCallback;
import com.customlbs.library.model.Building;
import com.customlbs.library.model.DebugInfo;
import com.customlbs.library.model.Zone;
import com.customlbs.shared.Coordinate;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        zones = new ArrayList<Zone>();
        IndoorsFactory.createInstance(this, "d2b8119f-49b4-4e21-a67b-67fa90a17b45", this, false);
        Log.d("oncreate", "LocationActivity");
//        textView = new TextView(this);
//        setContentView(R.layout.activity_location);
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
//        Toast.makeText(this, indoorsException.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("stopped", "");
        indoors.removeLocationListener(this);
        Toast.makeText(this, "indoo.rs stopped", Toast.LENGTH_SHORT).show();
        IndoorsFactory.releaseInstance(this);
    }

    @Override
    public void buildingLoaded(Building building) {
        Log.d("building loaded", building.toString());
        Toast.makeText(
                this,
                "Building is located at " + building.getLatOrigin() / 1E6 + ","
                        + building.getLonOrigin() / 1E6, Toast.LENGTH_SHORT).show();
//        indoors.enableEvaluationMode();
    }

    @Override
    public void changedFloor(int floorLevel, String name) {
    }

    @Override
    public void enteredZones(List<Zone> zones) {
        if(zones.size() > 0){
            this.zones = zones;
        }
        String s = "zones: ";
        for(Zone zone: this.zones){
            Log.d("zone", zone.toString());
            s = s + zone.getName();
        }
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
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
        String s = "user is located at " + userPosition.toString();
//        Toast.makeText(
//                this,
//                s, Toast.LENGTH_SHORT).show();
//        textView.setText(s);
//        setContentView(textView);
    }

    @Override
    public void setDebugInfo(DebugInfo debugInfo) {
        Log.d("DEBUGINFO", debugInfo.toString());
    }
}

