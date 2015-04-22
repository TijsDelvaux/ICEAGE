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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Ine on 18/03/2015.
 */
public class LocationActivity extends FragmentActivity implements IndoorsServiceCallback,
        IndoorsLocationListener, DebugInfoCallback{

//    private TextView textView;
    private Indoors indoors;
    private List<String> detectedZones;
    private List<String> zones;
    private Map<String, Set<String>> adjacentZones;
    private ImageTargets imageTargets;

    public LocationActivity(ImageTargets imageTargets){
        zones = new ArrayList<String>();
        detectedZones = new ArrayList<String>();
        this.imageTargets = imageTargets;
        set_adjacentZones();
        IndoorsFactory.createInstance(imageTargets, "d2b8119f-49b4-4e21-a67b-67fa90a17b45", this, false);
        Log.d("oncreate", "LocationActivity");
    }

    private void set_adjacentZones(){
        this.adjacentZones = new HashMap<String, Set<String>>();

        //foyer_wc
        Set<String> foyerWc = new HashSet<String>();
        foyerWc.add("foyer_automaten");
        this.adjacentZones.put("foyer_wc",foyerWc);
        // foyer_automaten
        Set<String> foyerAutomaten = new HashSet<String>();
        foyerAutomaten.add("foyer_wc");
        foyerAutomaten.add("automaten");
        foyerAutomaten.add("midden_foyer");
        this.adjacentZones.put("foyer_automaten",foyerAutomaten);
        // midden_foyer
        Set<String> middenFoyer = new HashSet<String>();
        middenFoyer.add("foyer_automaten");
        middenFoyer.add("automaten");
        middenFoyer.add("midden_foyer");
        middenFoyer.add("foyer_leslokaal_trappen");
        this.adjacentZones.put("midden_foyer",middenFoyer);
        //automaten
        Set<String> automaten = new HashSet<String>();
        automaten.add("midden_foyer");
        automaten.add("foyer_automaten");
        this.adjacentZones.put("automaten",automaten);
        //foyer_leslokaal_trappen
        Set<String> foyerLeslokaalTrappen = new HashSet<String>();
        foyerLeslokaalTrappen.add("midden_foyer");
        foyerLeslokaalTrappen.add("foyer_secr");
        this.adjacentZones.put("foyer_leslokaal_trappen",foyerLeslokaalTrappen);
        //foyer_secr
        Set<String> foyerSecr = new HashSet<String>();
        foyerSecr.add("foyer_leslokaal_trappen");
        foyerSecr.add("printerlokaal");
        foyerSecr.add("uitgang_secr");
        this.adjacentZones.put("foyer_secr",foyerSecr);
        //printerlokaal
        Set<String> printerlokaal = new HashSet<String>();
        printerlokaal.add("foyer_secr");
        this.adjacentZones.put("printerlokaal",printerlokaal);
        //uitgang_secr
        Set<String> uitgangSecr = new HashSet<String>();
        uitgangSecr.add("foyer_secr");
        this.adjacentZones.put("uitgang_secr",uitgangSecr);
        //gang_sols
        Set<String> gangSols = new HashSet<String>();
        gangSols.add("solZ");
        gangSols.add("solN");
        gangSols.add("foyer_secr");
        this.adjacentZones.put("gang_sols",gangSols);
        //solZ
        Set<String> solZ = new HashSet<String>();
        solZ.add("gang_sols");
        this.adjacentZones.put("solZ",solZ);
        //solN
        Set<String> solN = new HashSet<String>();
        solN.add("gang_sols");
        this.adjacentZones.put("solN",solN);

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
            List<String> zone_names = new ArrayList<String>();
            for(Zone zone: zones){
                zone_names.add(zone.getName());
            }
            Boolean new_zones = !(zone_names.containsAll(this.detectedZones) && this.detectedZones.containsAll(zone_names));
            if(new_zones){
                Log.d("LocationActivity", "new zones " + zone_names.toString());
                this.detectedZones = zone_names;
                Set<String> detectedAdjacentZones = new HashSet<String>();
                for(String zone: zone_names){
                    detectedAdjacentZones.add(zone);
                    detectedAdjacentZones.addAll(this.adjacentZones.get(zone));
                }
                this.zones = new ArrayList<String>(detectedAdjacentZones);
                this.imageTargets.enteredZones(this.zones);
            }
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

