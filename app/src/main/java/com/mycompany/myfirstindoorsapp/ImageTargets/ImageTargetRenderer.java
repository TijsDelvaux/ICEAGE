/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.mycompany.myfirstindoorsapp.ImageTargets;

import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mycompany.myfirstindoorsapp.SampleApplication.utils.Acorn;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.CubeShaders;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.LoadingDialogHandler;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.MeshObject;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.QuadMesh;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.SampleApplication3DModel;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.SampleApplicationSession;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.SampleUtils;
//import com.mycompany.myfirstindoorsapp.SampleApplication.utils.Teapot;
import com.mycompany.myfirstindoorsapp.SampleApplication.utils.Texture;
import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.Trackable;
import com.qualcomm.vuforia.TrackableResult;
import com.qualcomm.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.qualcomm.vuforia.Vuforia;


// The renderer class for the ImageTargets sample. 
public class ImageTargetRenderer implements GLSurfaceView.Renderer
{
    private static final String LOGTAG = "ImageTargetRenderer";
    
    private SampleApplicationSession vuforiaAppSession;
    private ImageTargets mActivity;
    
    private Vector<Texture> mTextures;
    
    private int shaderProgramID;
    
    private int vertexHandle;
    
    private int normalHandle;
    
    private int textureCoordHandle;
    
    private int mvpMatrixHandle;
    
    private int texSampler2DHandle;
    
    private Acorn acorn;
    private QuadMesh picture;
    private MeshObject objectToShow;
    private boolean acornTaken = false;

    private float kBuildingScale = 12.0f;
    private SampleApplication3DModel mBuildingsModel;
    
    private Renderer mRenderer;
    
    boolean mIsActive = false;
    
    private static final float OBJECT_SCALE_FLOAT = 100.0f;

    private HashSet<String> excludedImageSet = new HashSet<String>();
    private HashSet<String> freeImageSet = new HashSet<String>();
    private HashSet<String> myPickedUpSet = new HashSet<String>();
    private HashSet<String> placedTrap = new HashSet<String>();
    private String currentImage;
    private int askCount = 0;
    private int askCountLimit = 20;
    private long askTime = 0;
    private long askTimeLimit = 3000; //Waits at least 3 seconds before asking the server

    private static int IMG_ACORN_BROWN = 0;
    private static int IMG_SCRAT_EXCITED = 1;
    private static int IMG_SCRAT_HAPPY = 2;
    private static int IMG_SCRAT_SAD = 3;
    private static int IMG_SCRAP_TRAP = 4;

    private boolean inTrapState;

    public void setInTrapState(boolean state) {
        inTrapState = state;
    }


    //ICEAGE
    //Dit is de imageset die gebruikt wordt.
//    String imageSet = "foyer";
    
    public ImageTargetRenderer(ImageTargets activity,
        SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;
    }
    
    
    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;
        // Call our function to render content
        renderFrame();
    }
    
    
    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        
        initRendering();
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();
    }
    
    
    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);
    }
    
    
    // Function for initializing the renderer.
    private void initRendering()
    {
        acorn = new Acorn();
        picture = new QuadMesh(1.5,1);
        
        mRenderer = Renderer.getInstance();
        
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition");
        normalHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexNormal");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "texSampler2D");
        
        try
        {
            mBuildingsModel = new SampleApplication3DModel();
            mBuildingsModel.loadModel(mActivity.getResources().getAssets(),
                "ImageTargets/Buildings.txt");
        } catch (IOException e)
        {
            Log.e(LOGTAG, "Unable to load buildings");
        }
        
        // Hide the Loading Dialog
        mActivity.loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        
    }
    
    
    // The render function.
    private void renderFrame()
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        State state = mRenderer.begin();
        mRenderer.drawVideoBackground();
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        
        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

        if(state.getNumTrackableResults() == 0){
//            displayMessage("Nothing here!", 1);
            disableCollectButton();
            disableSetTrapButton();
        }

        if(inTrapState) {
            enableFellInTrap();
        }
        else {
            disableFellInTrap();
        }

        // did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
        {

            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            currentImage = trackable.getName();
            int textureIndex;

            if(myPickedUpSet.contains(currentImage)){ //YOU PICKED UP THIS ACORN
                if(placedTrap.contains(currentImage)){
                    disableSetTrapButton();
                    objectToShow = picture;
                    textureIndex = IMG_SCRAP_TRAP;
                }else{
                    enableSetTrapButton();
                    objectToShow = picture;
                    textureIndex = IMG_SCRAT_HAPPY;
                }
                disableCollectButton();
                objectToShow = picture;
            }else if(excludedImageSet.contains(currentImage)){ //SOMEONE ELSE PICKED UP THIS ACORN
                disableCollectButton();
                disableSetTrapButton();
                objectToShow = picture;
                textureIndex = IMG_SCRAT_SAD;
                long timeElapsed = System.currentTimeMillis() - askTime;
                if(Math.abs(timeElapsed) >= askTimeLimit) {
                    isTaken(currentImage);
                    askTime = System.currentTimeMillis();
                }
            }else if(!freeImageSet.contains(currentImage)) { //YOU'RE NOT SURE IF THIS ACORN HAS BEEN PICKED UP YET
                disableCollectButton();
                disableSetTrapButton();
                objectToShow = acorn;
                textureIndex = IMG_ACORN_BROWN;
//                askCount ++;
                long timeElapsed = System.currentTimeMillis() - askTime;
                if(Math.abs(timeElapsed) >= askTimeLimit) {
                    isTaken(currentImage);
                    askTime = System.currentTimeMillis();
                }

            }else{//THE ACORN HASN'T BEEN PICKED UP YET
                enableCollectButton();
                disableSetTrapButton();
                objectToShow = acorn;
                textureIndex = IMG_ACORN_BROWN;
                long timeElapsed = System.currentTimeMillis() - askTime;
                if(Math.abs(timeElapsed) >= askTimeLimit) {
                    isTaken(currentImage);
                    askTime = System.currentTimeMillis();
                }
            }







//            printUserData(trackable);
            Matrix44F modelViewMatrix_Vuforia = Tool
                .convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();
            
            // deal with the modelview and projection matrices
            float[] modelViewProjection = new float[16];
            
            if (!mActivity.isExtendedTrackingActive())
            {
                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                    OBJECT_SCALE_FLOAT);
                Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
                    OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
            } else
            {
                Matrix.rotateM(modelViewMatrix, 0, 90.0f, 1.0f, 0, 0);
                Matrix.scaleM(modelViewMatrix, 0, kBuildingScale,
                    kBuildingScale, kBuildingScale);
            }
            
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
            
            // activate the shader program and bind the vertex/normal/tex coords
            GLES20.glUseProgram(shaderProgramID);

            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, objectToShow.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                false, 0, objectToShow.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, objectToShow.getTexCoords());

            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);

            // activate texture 0, bind it, and pass to shader
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(textureIndex).mTextureID[0]);
            GLES20.glUniform1i(texSampler2DHandle, 0);

            // pass the model view matrix to the shader
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, objectToShow.getNumObjectVertex());

            // disable the enabled arrays
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

            
            SampleUtils.checkGLError("Render Frame");
            
        }
        
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        mRenderer.end();
    }
    
    
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
        
    }

    //ADDED ICEAGE STUFF

    // A handler object for sending messages to the main activity thread
    public static Handler ImageTargetHandler;

    // Called from native to display a message
    public void displayMessage(String text, int code)
    {
        // We use a handler because this thread cannot change the UI
        Message message = new Message();
        message.obj = text;
        message.what = code;
        ImageTargetHandler.sendMessage(message);
    }


    public void disableCollectButton(){
        displayMessage("Disabling collect button", 1);
    }

    public void enableCollectButton(){
        displayMessage("Enabling collect button", 2);
    }

    public void disableSetTrapButton(){
        displayMessage("Disabling set trap button", 4);
    }

    public void enableSetTrapButton(){
        displayMessage("Enabling set trap button", 5);
    }

    public void disableFellInTrap(){
        displayMessage("Disabling fell in trap", 6);
    }

    public void enableFellInTrap(){
        displayMessage("Enabling fell in trap", 7);
    }

    public void isTaken(String image){
        displayMessage(image, 3);
    }



    //This method should be called when pressing the "collect" button when an acorn is visible.
    //The picture then should be removed from the trackable list
    public String collectCurrentPicture(){
        myPickedUpSet.add(currentImage);
        return currentImage;
    }

    public void addToFreeSet(String image){
        freeImageSet.add(image);
    }

    public void addToExcludedSet(String image){
        excludedImageSet.add(image);
        if(freeImageSet.contains(image)){
            freeImageSet.remove(image);
        }
    }

    public void addToMyPickedUpSet(String image){
        myPickedUpSet.add(image);
    }

    public void removeFromMyPickedUpSet(String image){
        myPickedUpSet.remove(image);
    }


    public void addToTraps(){
        placedTrap.add(currentImage);
    }

    public void removeTrap(String image){
        placedTrap.remove(image);
    }

}
