/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.mycompany.myfirstindoorsapp.ImageTargets;

import server.MsgClient;
import server.MsgServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.myfirstindoorsapp.IceAge;
import com.mycompany.myfirstindoorsapp.LocationActivity;
import com.mycompany.myfirstindoorsapp.R;
import com.mycompany.myfirstindoorsapp.R.string;
import com.mycompany.myfirstindoorsapp.SampleAppMenu.SampleAppMenu;
import com.mycompany.myfirstindoorsapp.SampleAppMenu.SampleAppMenuGroup;
import com.mycompany.myfirstindoorsapp.SampleAppMenu.SampleAppMenuInterface;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.LoadingDialogHandler;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.SampleApplicationControl;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.SampleApplicationException;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.SampleApplicationGLView;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.SampleApplicationSession;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.Texture;
import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.DataSet;
import com.qualcomm.vuforia.ObjectTracker;
import com.qualcomm.vuforia.STORAGE_TYPE;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Trackable;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vuforia;


public class ImageTargets extends Activity implements SampleApplicationControl,
    SampleAppMenuInterface
{
    private static final String LOGTAG = "ImageTargets";
    
    SampleApplicationSession vuforiaAppSession;
    
    private DataSet mCurrentDataset;
    private int mCurrentDatasetSelectionIndex = 0;
    private int mStartDatasetsIndex = 0;
    private int mDatasetsNumber = 0;
    private ArrayList<String> mDatasetStrings = new ArrayList<String>();
    
    // Our OpenGL view:
    private SampleApplicationGLView mGlView;
    
    // Our renderer:
    private ImageTargetRenderer mRenderer;
    
    private GestureDetector mGestureDetector;
    
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    
    private boolean mSwitchDatasetAsap = false;
    private boolean mFlash = false;
    private boolean mContAutofocus = false;
    private boolean mExtendedTracking = false;
    
    private View mFlashOptionView;
    private TextView mPlayerCollectedAcornsView;
    private TextView mTeamCollectedAcornsView;
    private TextView mCurrentZonesView;
    private TextView mLastReceivedMessage;

    private String lastReceivedMessage = "";
    
    private RelativeLayout mUILayout;
    
    private SampleAppMenu mSampleAppMenu;
    
    LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;
    
    boolean mIsDroidDevice = false;

    //ICEAGE variables
    private String[] listDatasetStrings;
    private Map<String,Integer> allZones;
    private String zonesString = "";
    private DataSet[] listDatasets;

    private RelativeLayout countLayout;
    private RelativeLayout snowLayout;
    private RelativeLayout settrapLayout;
    private RelativeLayout fellIntoTrapLayout;
    private View collectButton;
    private View setTrapButton;

    private boolean showCollectButton;
    private boolean showSetTrapButton;
    private boolean showWalkedInTrap;


    @IceAge
    private void init() {
        listDatasetStrings = new String[] {"StonesAndChips.xml", // index 0: default
                                           "automaten.xml",
                                           "foyer_automaten.xml",
                                           "foyer_wc.xml",
                                           "midden_foyer.xml",
                                           "foyer_leslokaal_trappen.xml",
                                           "foyer_secr.xml",
                                           "uitgang_secr.xml",
                                           "gang_sols.xml",
                                           "solZ.xml",
                                           "solN.xml",
                                           "printerlokaal.xml"};

        allZones = new HashMap<String,Integer>();
        allZones.put("automaten", 1); // index 0 = default
        allZones.put("foyer_automaten", 2);
        allZones.put("foyer_wc", 3);
        allZones.put("midden_foyer", 4);
        allZones.put("foyer_leslokaal_trappen", 5);
        allZones.put("foyer_secr", 6);
        allZones.put("uitgang_secr", 7);
        allZones.put("gang_sols", 8);
        allZones.put("solZ", 9);
        allZones.put("solN", 10);
        allZones.put("printerlokaal", 10);
        //TODO: zones toevoegen

        Log.d(LOGTAG,"after targets init");
    }

    private int playerCollectedAcorns;
    private int teamCollectedAcorns;
    private String serverIP;
    private String userName;
    private String teamName;
    private int port;
    private int playerColor;
    private int teamColor;
    private ClientTask clientask;
    private Stack<String> msgsToServer;
    private long blindedTime = 10000;


//    private Socket client;
//    NetworkTask networktask;

    // Called when the activity first starts or the user navigates back to an
    // activity.
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        init();

        Log.d(LOGTAG, "\n\n================================================\nImageTargets: onCreate\n");
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();
        serverIP = b.getString("ip");
        userName = b.getString("username");
        teamName = b.getString("teamname");
        port = 4444;
        msgsToServer = new Stack<String>();
        clientask = new ClientTask(serverIP, port,this);
        clientask.start();

        playerColor = getResources().getColor(R.color.blue);
        teamColor = getResources().getColor(R.color.red);

        sendMessageToServer(MsgServer.REGISTER, "Hello");
        
        vuforiaAppSession = new SampleApplicationSession(this);
        startLoadingAnimation();

        mDatasetStrings.add("Thuis.xml");

        vuforiaAppSession
            .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        mGestureDetector = new GestureDetector(this, new GestureListener());
        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
        loadTextures(); // loads the teapot
        Log.d(LOGTAG,"After loadTextures");
        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
            "droid");


        //ICEAGE
        setTargetsToFollow(new ArrayList<String>());

        showCollectButton = false;
        showSetTrapButton = false;
        addOverlayView();

        Log.d(LOGTAG, "Vuforia end of onCreate");
        new LocationActivity(this);

    }


    @IceAge
    public void onClickCollectButton(View view){
        String currentImage = mRenderer.collectCurrentPicture();
//        playerCollectedAcorns++;
//        teamCollectedAcorns++;
        sendMessageToServer(MsgServer.ACORN_PICKUP, currentImage);
    }

    @IceAge
    public void onClickSetTrapButton(View view){
        String currentImage = mRenderer.collectCurrentPicture();
        sendMessageToServer(MsgServer.SET_TRAP, currentImage);
    }

    public void setClientCollectedAcorns(int count){
        playerCollectedAcorns = count;
    }

    public void setTeamCollectedAcorns(int count){
        teamCollectedAcorns = count;
    }

    public void walkIntoTrap(){
        mRenderer.setInTrapState(true);

        Long startTime = System.currentTimeMillis();

        while((Math.abs(System.currentTimeMillis() - startTime) < blindedTime)){
            Log.d(LOGTAG, "[TRAP]");
        }

        mRenderer.setInTrapState(false);
    }


    //END ICE STUFF

    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
        GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();
        
        
        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }
        
        
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            // Generates a Handler to trigger autofocus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                        CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
                    
                    if (!result)
                        Log.e("SingleTapUp", "Unable to trigger focus");
                }
            }, 1000L);
            
            return true;
        }
    }
    
    
    // We want to load specific textures from the APK, which we will later use
    // for rendering.
    
    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk("TexturebruineEikel.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("scrat_excited.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("scrat_happy_text.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("scrat_sad_text.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("scrat_trap_text.png",
                getAssets()));
    }
    
    
    // Called when the activity will start interacting with the user.
    @Override
    @IceAge
    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        //ICEAGE
        // Create a new handler for the renderer thread to use
        // This is necessary as only the main thread can make changes to the UI
        mRenderer.ImageTargetHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0: //show a toast with contents of the message (ex: # acorns  gathered)
                        showToastImageTargets((String) msg.obj);
                        break;
                    case 1: //Hide the collect button
                        collectButton.setVisibility(View.INVISIBLE);
                        showCollectButton = false; //TODO is deze boolean nodig?
                        break;
                    case 2: //Show the collect button
                        collectButton.setVisibility(View.VISIBLE);
                        showCollectButton = true;
                        break;
                    case 3: //Check if the detected image already has been taken
                        sendMessageToServer(MsgServer.ACORN_REQUEST, (String) msg.obj);
                        break;
                    case 4:
                        if(!(setTrapButton == null)) {
                            setTrapButton.setVisibility(View.INVISIBLE);
                        }
                        showSetTrapButton = false;
                        break;
                    case 5:
                        if(!(setTrapButton == null)) {
                            setTrapButton.setVisibility(View.VISIBLE);
                        }
                        showSetTrapButton = true;
                        break;
                    case 6:
                        if(!(fellIntoTrapLayout == null)) {
                            fellIntoTrapLayout.setVisibility(View.INVISIBLE);
                        }
                        showWalkedInTrap = false;
                        break;
                    case 7:
                        if(!(fellIntoTrapLayout == null)) {
                            fellIntoTrapLayout.setVisibility(View.VISIBLE);
                        }
                        showWalkedInTrap = true;
                        break;
                    default:
//                        Log.d("ImageTargetHandler", "Nothing");
                        break;
                }
            }
        };
        //End ICEAGE


        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        
        try
        {
            addOverlayView();
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Resume the GL view:
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    
    
    // Callback for configuration changes the activity handles itself
    @Override
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        vuforiaAppSession.onConfigurationChanged();

        // Removes the current layout and inflates a proper layout
        // for the new screen orientation

        if (mUILayout != null)
        {
            mUILayout.removeAllViews();
            ((ViewGroup) mUILayout.getParent()).removeView(mUILayout);

        }

        addOverlayView();
    }
    
    
    // Called when the system is about to start resuming a previous activity.
    @Override
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
        
        // Turn off the flash
        if (mFlashOptionView != null && mFlash)
        {
            // OnCheckedChangeListener is called upon changing the checked state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            {
                ((Switch) mFlashOptionView).setChecked(false);
            } else
            {
                ((CheckBox) mFlashOptionView).setChecked(false);
            }
        }
        
        try
        {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
    }
    
    
    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();
        
        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Unload texture:
        mTextures.clear();
        mTextures = null;
        
        System.gc();
    }
    
    
    // Initializes AR application components.
    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);
        
        mRenderer = new ImageTargetRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);


        //ICEAGE ADDED
        addOverlayView();
    }

    @IceAge
    private void setTargetsToFollow(List<String> zones) {
//        mDatasetStrings.clear();
//
//        for (String zone : zones) {
//            int zoneIndex = allZones.get(zone);
//            mDatasetStrings.add(listDatasetStrings[zoneIndex]);
//        }
//
//        if (mDatasetStrings.isEmpty()) {
//            mDatasetStrings.add(listDatasetStrings[0]);
//        }

        vuforiaAppSession.doReloadTargets();
    }

    @IceAge
    public void enteredZones(List<String> zones){
        setTargetsToFollow(zones); // zet de juiste targets

        String s = "Zones:\n";
        for(String zone: zones){
            s = s + " - " + zone + "\n";
        }
        Log.d(LOGTAG,s);

        zonesString = s;
        menuProcess(CMD_CURRENT_ZONES);
    }

    @IceAge
    private void addOverlayView(){
//        Log.d("addOverlayView", "showCollectButton: " + showCollectButton);
        // Inflates the Overlay Layout to be displayed above the Camera View
        LayoutInflater inflater = LayoutInflater.from(this);
        countLayout = (RelativeLayout) inflater.inflate(R.layout.collect_overlay, null, false);
        snowLayout = (RelativeLayout) inflater.inflate(R.layout.snow_overlay, null, false);
        settrapLayout = (RelativeLayout) inflater.inflate(R.layout.set_trap_overlay, null, false);
        fellIntoTrapLayout = (RelativeLayout) inflater.inflate(R.layout.fell_into_trap_overlay, null, false);

        snowLayout.setVisibility(View.VISIBLE);
        countLayout.setVisibility(View.VISIBLE);
        settrapLayout.setVisibility(View.VISIBLE);
        fellIntoTrapLayout.setVisibility(View.INVISIBLE);

        // Adds the inflated layout to the view
        addContentView(snowLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        addContentView(countLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        addContentView(settrapLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        addContentView(fellIntoTrapLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        collectButton = countLayout.findViewById(R.id.collect_overlay);
        setTrapButton = settrapLayout.findViewById(R.id.set_trap_button);

        if(showCollectButton){
            collectButton.setVisibility(View.VISIBLE);
        } else {
            collectButton.setVisibility(View.INVISIBLE);
        }

        if(showSetTrapButton){
            setTrapButton.setVisibility(View.VISIBLE);
        } else {
            setTrapButton.setVisibility(View.INVISIBLE);
        }

        if(showWalkedInTrap){
            fellIntoTrapLayout.setVisibility(View.VISIBLE);
        } else {
            fellIntoTrapLayout.setVisibility(View.INVISIBLE);
        }
    }


    private void startLoadingAnimation()
    {
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay,
            null, false);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);


        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_indicator);
        
        // Shows the loading indicator at start
        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        
        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
    }
    
    
    // Methods to load and destroy tracking data.
    @Override
    public boolean doLoadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;
        
        if (listDatasets != null) {
            doUnloadTrackersData();
        }

        listDatasets = new DataSet[mDatasetStrings.size()];

        for(int i = 0; i < mDatasetStrings.size(); i++) {
            mCurrentDatasetSelectionIndex = i;
            listDatasets[i] = objectTracker.createDataSet();

            if (listDatasets[i] == null)
                return false;

            if (!listDatasets[i].load(
                    mDatasetStrings.get(mCurrentDatasetSelectionIndex),
                    STORAGE_TYPE.STORAGE_APPRESOURCE))
                return false;

            if (!objectTracker.activateDataSet(listDatasets[i]))
                result = false;

            int numTrackables = listDatasets[i].getNumTrackables();

            String dataString = "";
            for (int count = 0; count < numTrackables; count++)
            {
                Trackable trackable = listDatasets[i].getTrackable(count);
                if(isExtendedTrackingActive())
                {
                    trackable.startExtendedTracking();
                }

                String name = "Current Dataset : " + trackable.getName();
                trackable.setUserData(name);
                dataString += trackable.getName() + " ";
            }
            Log.d(LOGTAG,"=====Current Dataset : " + dataString);
        }
        mCurrentDatasetSelectionIndex = 0;

        Log.d(LOGTAG,objectTracker.getActiveDataSet().toString());
        return result;
    }
    
    
    @Override
    public boolean doUnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;
        
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;
        
        if (listDatasets != null)
        {
            // deactivate and destroy all current datasets
            for(DataSet data: listDatasets) {
                if(!objectTracker.deactivateDataSet(data)) result = false;
            }

            listDatasets = null;
        }
        
        return result;
    }
    
    
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        
        if (exception == null)
        {
            initApplicationAR();
//            showCollectButton = true;
            mRenderer.mIsActive = true;
            
            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
            
            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();
            
            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);
            
            try
            {
                addOverlayView();
                vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
            } catch (SampleApplicationException e)
            {
                Log.e(LOGTAG, e.getString());
            }
            
            boolean result = CameraDevice.getInstance().setFocusMode(
                CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
            
            if (result)
                mContAutofocus = true;
            else
                Log.e(LOGTAG, "Unable to enable continuous autofocus");
            
            mSampleAppMenu = new SampleAppMenu(this, this, getString(string.app_name),
                mGlView, mUILayout, null);
            setSampleAppMenuSettings();

        } else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
//        showCollectButton = false;
    }
    
    
    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }
                
                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                    ImageTargets.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle(getString(R.string.INIT_ERROR))
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton(getString(R.string.button_OK),
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                finish();
                            }
                        });
                
                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }
    
    
    @Override
    public void onQCARUpdate(State state)
    {
        if (mSwitchDatasetAsap)
        {
            mSwitchDatasetAsap = false;
            TrackerManager tm = TrackerManager.getInstance();
            ObjectTracker ot = (ObjectTracker) tm.getTracker(ObjectTracker
                .getClassType());
            if (ot == null || mCurrentDataset == null
                || ot.getActiveDataSet() == null)
            {
                Log.d(LOGTAG, "Failed to swap datasets");
                return;
            }
            
            doUnloadTrackersData();
            doLoadTrackersData();
        }
    }
    
    
    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;
        
        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;
        
        // Trying to initialize the image tracker
        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
        {
            Log.e(
                LOGTAG,
                "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }
        return result;
    }
    
    
    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;
        
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
            ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();

        return result;
    }
    
    
    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;
        
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
            ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();
        
        return result;
    }
    
    
    @Override
    public boolean doDeinitTrackers()
    {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;
        
        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());
        
        return result;
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Process the Gestures
        if (mSampleAppMenu != null && mSampleAppMenu.processEvent(event))
            return true;


        return mGestureDetector.onTouchEvent(event);
    }
    
    
    boolean isExtendedTrackingActive()
    {
        return mExtendedTracking;
    }
    
    final public static int CMD_BACK = -1;
    final public static int CMD_AUTOFOCUS = 1;
    final public static int CMD_FLASH = 2;
    final public static int CMD_CAMERA_FRONT = 3;
    final public static int CMD_CAMERA_REAR = 4;
    final public static int CMD_UPDATE_COUNT = 5;
    final public static int CMD_CURRENT_ZONES = 6;
    final public static int CMD_NOTHING = 7;
    final public static int CMD_UPDATE_LAST_TEXT = 8;
    
    
    // This method sets the menu's settings
    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;

        group = mSampleAppMenu.addGroup("", false);
        TextView userNameView =(TextView) group.addTextItem("UserName: " + userName, CMD_NOTHING);
        userNameView.setTextColor(playerColor);
        TextView teamNameView =(TextView) group.addTextItem("Team: " + teamName, CMD_NOTHING);
        teamNameView.setTextColor(teamColor);

//        group = mSampleAppMenu.addGroup("", true);

        //Shows the amount of collected acorns
        group.addTextItem("Number of collected acorns: ", CMD_NOTHING);
        mPlayerCollectedAcornsView = (TextView) group.addTextItem("You: " + playerCollectedAcorns, CMD_UPDATE_COUNT);
        mPlayerCollectedAcornsView.setTextColor(playerColor);
        mTeamCollectedAcornsView = (TextView) group.addTextItem("Your team: " + teamCollectedAcorns, CMD_UPDATE_COUNT);
        mTeamCollectedAcornsView.setTextColor(teamColor);

        mLastReceivedMessage = (TextView) group.addTextItem(lastReceivedMessage, CMD_UPDATE_LAST_TEXT);
        //Show the list of zones you're currently in
        mCurrentZonesView = (TextView) group.addTextItem(zonesString, CMD_CURRENT_ZONES);
//        group.addSelectionItem(getString(R.string.menu_extended_tracking),
//            CMD_EXTENDED_TRACKING, false);
        group.addSelectionItem(getString(R.string.menu_contAutofocus),
            CMD_AUTOFOCUS, mContAutofocus);
        mFlashOptionView = group.addSelectionItem(
            getString(R.string.menu_flash), CMD_FLASH, false);
        
        CameraInfo ci = new CameraInfo();
        boolean deviceHasFrontCamera = false;
        boolean deviceHasBackCamera = false;
        for (int i = 0; i < Camera.getNumberOfCameras(); i++)
        {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_FRONT)
                deviceHasFrontCamera = true;
            else if (ci.facing == CameraInfo.CAMERA_FACING_BACK)
                deviceHasBackCamera = true;
        }
        
        if (deviceHasBackCamera && deviceHasFrontCamera)
        {
            group = mSampleAppMenu.addGroup(getString(R.string.menu_camera),
                true);
            group.addRadioItem(getString(R.string.menu_camera_front),
                CMD_CAMERA_FRONT, false);
            group.addRadioItem(getString(R.string.menu_camera_back),
                CMD_CAMERA_REAR, true);
        }

        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);


        mSampleAppMenu.attachMenu();
    }
    
    
    @Override
    public boolean menuProcess(int command)
    {
        boolean result = true;
        
        switch (command)
        {
            case CMD_BACK:
                finish();
                break;
            
            case CMD_FLASH:
                result = CameraDevice.getInstance().setFlashTorchMode(!mFlash);
                
                if (result)
                {
                    mFlash = !mFlash;
                } else
                {
                    showToastImageTargets(getString(mFlash ? R.string.menu_flash_error_off
                            : R.string.menu_flash_error_on));
                    Log.e(LOGTAG,
                        getString(mFlash ? R.string.menu_flash_error_off
                            : R.string.menu_flash_error_on));
                }
                break;
            
            case CMD_AUTOFOCUS:
                
                if (mContAutofocus)
                {
                    result = CameraDevice.getInstance().setFocusMode(
                        CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
                    
                    if (result)
                    {
                        mContAutofocus = false;
                    } else
                    {
                        showToastImageTargets(getString(R.string.menu_contAutofocus_error_off));
                        Log.e(LOGTAG,
                            getString(R.string.menu_contAutofocus_error_off));
                    }
                } else
                {
                    result = CameraDevice.getInstance().setFocusMode(
                        CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
                    
                    if (result)
                    {
                        mContAutofocus = true;
                    } else
                    {
                        showToastImageTargets(getString(R.string.menu_contAutofocus_error_on));
                        Log.e(LOGTAG,
                            getString(R.string.menu_contAutofocus_error_on));
                    }
                }
                
                break;
            
            case CMD_CAMERA_FRONT:
            case CMD_CAMERA_REAR:
                
                // Turn off the flash
                if (mFlashOptionView != null && mFlash)
                {
                    // OnCheckedChangeListener is called upon changing the checked state
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                    {
                        ((Switch) mFlashOptionView).setChecked(false);
                    } else
                    {
                        ((CheckBox) mFlashOptionView).setChecked(false);
                    }
                }
                
                vuforiaAppSession.stopCamera();
                
                try
                {
                    addOverlayView();
                    vuforiaAppSession
                        .startAR(command == CMD_CAMERA_FRONT ? CameraDevice.CAMERA.CAMERA_FRONT
                                : CameraDevice.CAMERA.CAMERA_BACK);
                } catch (SampleApplicationException e)
                {
                    showToastImageTargets(e.getString());
                    Log.e(LOGTAG, e.getString());
                    result = false;
                }
                doStartTrackers();
                break;

            case CMD_UPDATE_COUNT:
                mPlayerCollectedAcornsView.setText("You: " + playerCollectedAcorns);
                mTeamCollectedAcornsView.setText("Your team: " + teamCollectedAcorns);
                break;

            case CMD_CURRENT_ZONES:
                String zones = zonesString;
                if(zonesString.equals("") || zonesString == null){
                    zones = "You're not in an acknowledged zone";
                }
                if(!(mCurrentZonesView == null)){
                    mCurrentZonesView.setText(zones);
                }
                break;
            case CMD_NOTHING:
                break;
            case CMD_UPDATE_LAST_TEXT:
                mLastReceivedMessage.setText(lastReceivedMessage);
                break;
            default:
                if (command >= mStartDatasetsIndex
                    && command < mStartDatasetsIndex + mDatasetsNumber)
                {
                    mSwitchDatasetAsap = true;
                    mCurrentDatasetSelectionIndex = command
                        - mStartDatasetsIndex;
                }
                break;
        }
        return result;
    }
    
    //Shows a standard toast with a shoret length
    private void showToastImageTargets(String text)
    {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
//        showToastImageTargets(text, Toast.LENGTH_SHORT);

    }

    //Shows a toast with variable length
    private void showToastImageTargets(String text, int duration) {
        Toast.makeText(this, text, duration).show();
    }


    /*
     * Send a message to the server
     */
    @IceAge
    public void sendMessageToServer(MsgServer code, String message){
        String userMessage = userName + ":" + teamName + ":" + code + ":" + message;
        Log.d(LOGTAG, "message toegevoegd: " + userMessage);
        msgsToServer.push(userMessage);
    }

    @IceAge
    public class ClientTask extends Thread {

        String serverAddress;
        int serverPort;
        ImageTargets imageTargets;

        ClientTask(String addr, int port, ImageTargets imgTargets) {
            serverAddress = addr;
            serverPort = port;
            this.imageTargets = imgTargets;

        }

        private void showToast(final String text, final int duration){
//            lastReceivedMessage = text;
//            menuProcess(CMD_UPDATE_LAST_TEXT);
            new Thread()
            {
                public void run()
                {
                    ImageTargets.this.runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            Toast.makeText(getApplicationContext(), text, duration).show();

                        }
                    });
                }
            }.start();
        }
        private void showToast(final String text){
            showToast(text, Toast.LENGTH_LONG);
        }

        /*
         * Running thread
         */
        @Override
        public void run() {
            Log.d(LOGTAG, "in run " );
            String response = "";
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            Stack<String> responses = new Stack<String>();

            try {
                socket = new Socket(serverAddress, serverPort);
                (new ResponseGetter(socket, responses)).start();
                while(true){
                    dataOutputStream = new DataOutputStream(
                            socket.getOutputStream());
                    // wait until you have a message to send
                    while(!msgsToServer.empty()){
                        dataOutputStream.writeUTF(msgsToServer.pop());
                    }
                    // wait for a response
                    while(!responses.empty()){
                        handleResponse(responses.pop());
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                e.printStackTrace();
                response = "IOException: " + e.toString();
            }
        }

        /*
         * Handle a message from the server
         */
        @IceAge
        protected void handleResponse(String response) {
            Log.d("ClientComm", response);
            try {
                String[] splitResponse = response.split(":");
                String responseCode = splitResponse[0];
                String rsp = splitResponse[1];

                switch (MsgClient.valueOf(responseCode)) {
                    //Don't do anything
                    case DEFAULT:
                        break;
                    //Show a toast
                    case TOAST:
                        showToast(rsp);
                        break;

                    //Registration went well
                    case CONFIRM_REGISTRATION:
                        showToast(rsp, Toast.LENGTH_LONG);
                        String clientCount = splitResponse[2];
                        setClientCollectedAcorns(Integer.parseInt(clientCount));
                        String teamCount = splitResponse[3];
                        setTeamCollectedAcorns(Integer.parseInt(teamCount));
                        menuProcess(CMD_UPDATE_COUNT);
                        showToast("Swipe from left to right to show menu " +
                                "\n------->", Toast.LENGTH_LONG);
                        break;
                    // Registration did not went well
                    case DECLINE_REGISTRATION:
                        showToast(rsp); //TODO
                        break;

                    // Update the list of excluded images
                    case UPDATE_EXCLUDE_LIST:
                        updateExcludedList(rsp);
                        break;

                    // Reply from isTaken: there is an acorn here
                    case CONFIRM_ACORN:
                        mRenderer.addToFreeSet(rsp);
                        break;
                    // Reply from isTaken: there is NO acorn here
                    case DECLINE_ACORN:
                        mRenderer.addToExcludedSet(rsp);
                        break;

                    // You have successfully picked up an acorn
                    case CONFIRM_PICKUP:
                        showToast(rsp);
                        String imageToPickUp = splitResponse[2];
                        mRenderer.addToMyPickedUpSet(imageToPickUp);
                        clientCount = splitResponse[3];
                        setClientCollectedAcorns(Integer.parseInt(clientCount));
                        teamCount = splitResponse[4];
                        setTeamCollectedAcorns(Integer.parseInt(teamCount));
                        menuProcess(CMD_UPDATE_COUNT);
                        break;
                    //The player already owns this acorn
                    case YOU_OWN_THIS_ACORN:
                        mRenderer.addToMyPickedUpSet(rsp);
                        clientCount = splitResponse[2];
                        setClientCollectedAcorns(Integer.parseInt(clientCount));
                        teamCount = splitResponse[3];
                        setTeamCollectedAcorns(Integer.parseInt(teamCount));
                        menuProcess(CMD_UPDATE_COUNT);
                        break;
                    // Something went wrong while picking up an acorn
                    case DECLINE_PICKUP:
                        showToast(rsp); //TODO new request for acorn (maybe someone else has taken it in the meantime)
                        mRenderer.removeFromMyPickedUpSet(splitResponse[2]);
                        break;
                    // A team mate has picked up an acorn
                    case TEAMMATE_PICKUP:
                        Log.d("CLIENTTASK", "ontvangen: " + rsp);
                        showToast(rsp);
                        teamCount = splitResponse[2];
                        setTeamCollectedAcorns(Integer.parseInt(teamCount));
                        menuProcess(CMD_UPDATE_COUNT);

                    // Reply from isTaken: there is a trap here and you walked right into it!
                    case TRAP_LOSS:
                        showToast(rsp);
                        clientCount = splitResponse[2];
                        setClientCollectedAcorns(Integer.parseInt(clientCount));
                        teamCount = splitResponse[3];
                        setTeamCollectedAcorns(Integer.parseInt(teamCount));
                        menuProcess(CMD_UPDATE_COUNT);
                        walkIntoTrap();
                        break;
                    // A teammate of yours had walked into a trap
                    case TEAMMATE_TRAP_LOSS:
                        showToast(rsp);
                        teamCount = splitResponse[2];
                        setTeamCollectedAcorns(Integer.parseInt(teamCount));
                        menuProcess(CMD_UPDATE_COUNT);
                        break;
                    // Someone walked into your trap!
                    case TRAP_REWARD:
                        showToast(rsp);
                        clientCount = splitResponse[2];
                        setClientCollectedAcorns(Integer.parseInt(clientCount));
                        teamCount = splitResponse[3];
                        setTeamCollectedAcorns(Integer.parseInt(teamCount));
                        menuProcess(CMD_UPDATE_COUNT);
                        break;
                    // A teammate of yours had walked into a trap
                    case TEAMMATE_TRAP_REWARD:
                        showToast(rsp);
                        teamCount = splitResponse[2];
                        setTeamCollectedAcorns(Integer.parseInt(teamCount));
                        menuProcess(CMD_UPDATE_COUNT);
                        break;
                    case CONFIRM_PLACEMENT_TRAP:
                        mRenderer.addToTraps();
                        showToast(rsp);
                        clientCount = splitResponse[2];
                        setClientCollectedAcorns(Integer.parseInt(clientCount));
                        teamCount = splitResponse[3];
                        setTeamCollectedAcorns(Integer.parseInt(teamCount));
                        menuProcess(CMD_UPDATE_COUNT);
                        break;
                    case DECLINE_PLACEMENT_TRAP:
                        showToast(rsp);
                        break;

                    default:
                        break;

                }
            }catch(Exception e){
                Log.e("CLIENTTASK",e.getMessage() + "\nResponse: " + response);
            }
        }

    }

    public void updateExcludedList(String excludes){
        String[] list = excludes.split(";");
        //TODO the rest

    }

    public class ResponseGetter extends Thread{
        Socket socket;
        Stack<String> responses;

        public ResponseGetter(Socket socket, Stack<String> responses){
            this.socket = socket;
            this.responses = responses;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            while(true) {
                try {
                    dataInputStream = new DataInputStream(this.socket.getInputStream());
                    String response = dataInputStream.readUTF();
                    Log.d("ClientComm", "response: " + response);
                    this.responses.push(response);
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }
        }

    }
}
