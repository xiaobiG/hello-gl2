package com.android.gl2jni;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraOESRenderer implements GLSurfaceView.Renderer {

    private final GLSurfaceView mGLSurfaceView;
    // Camera 纹理
    private SurfaceTexture mSurfaceTexture = null;
    private int cameraTextureId = -1;

    private final float[] cameraTransformMatrix = new float[16];
    private FilterEngine mFilterEngine;

    private GLSurfaceView.Renderer rendererListener;

    public CameraOESRenderer(GLSurfaceView surfaceView) {
        mGLSurfaceView = surfaceView;
    }

    public void setRendererListener(GLSurfaceView.Renderer listener){
        this.rendererListener = listener;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
            // 获取用于渲染纹理的变换矩阵。这个矩阵由 SurfaceTexture 内部维护，它用于处理摄像头预览数据和纹理坐标之间的映射关系。
            // 如果没有正确地获取和使用这个矩阵，可能会导致图像出现翻转或拉伸等问题。
            mSurfaceTexture.getTransformMatrix(cameraTransformMatrix);
        }

        gl.glViewport(0, 0, mGLSurfaceView.getWidth(), mGLSurfaceView.getHeight());
        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        gl.glClearColor(1f, 1f, 0f, 0f);

        if (mFilterEngine == null) {
            mFilterEngine = new FilterEngine(cameraTextureId, mGLSurfaceView.getContext());
        }
        mFilterEngine.drawTexture(cameraTransformMatrix);

//        GL2JNILib.step();

        if (this.rendererListener != null) {
            this.rendererListener.onDrawFrame(gl);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GL2JNILib.init(width, height);

        if (this.rendererListener != null) {
            this.rendererListener.onSurfaceChanged(gl, width, height);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("onSurfaceCreated", "");
        cameraTextureId = createOESTextureObject();
        mSurfaceTexture = new SurfaceTexture(cameraTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                // 每获取到一帧数据时请求OpenGL ES进行渲染
                mGLSurfaceView.requestRender();
            }
        });

        if (this.rendererListener != null) {
            this.rendererListener.onSurfaceCreated(gl, config);
        }

    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    /**
     * 创建一个外部纹理用于接收预览数据
     */
    public static int createOESTextureObject() {
        int[] tex = new int[1];
        //生成一个纹理
        GLES20.glGenTextures(1, tex, 0);
        //将此纹理绑定到外部纹理上
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        //设置纹理过滤参数
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        //解除纹理绑定
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return tex[0];
    }
}
