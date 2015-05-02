package com.mycompany.myfirstindoorsapp.SampleApplication.utils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Tijs on 2/05/2015.
 */
public class QuadMesh extends MeshObject{

    private Buffer mVertBuff;
    private Buffer mTexCoordBuff;
    private Buffer mNormBuff;

    private int verticesNumber = 0;

    private double x;
    private double y;

    public QuadMesh(double x, double y)
    {
        this.x = x;
        this.y = y;
        setVerts();
        setTexCoords();
        setNorms();
    }

    public void setVerts(){
        double[] QUAD_VERTS =  {
                -x/2, -y/2, -2, //bottom-left corner
                x/2, -y/2, -2, //bottom-right corner
                x/2, y/2, -2, //top-right corner
                x/2, y/2, -2, //top-right corner
                -x/2, y/2, -2, //top-left corner
                -x/2, -y/2, -2 //bottom-left corner
        };
        mVertBuff = fillBuffer(QUAD_VERTS);
        verticesNumber = QUAD_VERTS.length / 3;
    }

    public void setTexCoords(){
        double[] QUAD_TEX_COORDS = {
                0.0, 0.0, //tex-coords at bottom-left corner
                1.0, 0.0, //tex-coords at bottom-right corner
                1.0, 1.0, //tex-coords at top-right corner
                1.0, 1.0, //tex-coords at top-right corner
                0.0, 1.0, //tex-coords at top-left corner
                0.0, 0.0 //tex-coords at bottom-left corner
        };
        mTexCoordBuff = fillBuffer(QUAD_TEX_COORDS);
    }

    public void setNorms(){
        double[] QUAD_NORMS = {
                0.0, 0.0, 1.0, //normal at bottom-left corner
                0.0, 0.0, 1.0, //normal at bottom-right corner
                0.0, 0.0, 1.0, //normal at top-right corner
                0.0, 0.0, 1.0, //normal at top-right corner
                0.0, 0.0, 1.0, //normal at top-left corner
                0.0, 0.0, 1.0 //normal at bottom-left corner
        };
        mNormBuff = fillBuffer(QUAD_NORMS);
    }

    @Override
    public int getNumObjectVertex()
    {
        return verticesNumber;
    }

    @Override
    public int getNumObjectIndex() {
        return 0;
    }


    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType)
    {
        Buffer result = null;
        switch (bufferType)
        {
            case BUFFER_TYPE_VERTEX:
                result = mVertBuff;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                result = mTexCoordBuff;
                break;
            case BUFFER_TYPE_NORMALS:
                result = mNormBuff;
                break;
            default:
                break;

        }

        return result;
    }


}