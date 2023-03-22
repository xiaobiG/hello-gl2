/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gl2jni;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class GL2JNIActivity extends Activity {

    GL2JNIView mView;
    CameraV1 mCamera;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        mView = findViewById(R.id.gl_view);

        mCamera = new CameraV1(this);
        mView.setRendererListener(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                mCamera.setPreviewTexture(mView.getPreviewSurfaceTexture());
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                Log.d("onSurfaceChanged", "view wh " + width + "," + height);
                Camera.Size size = CameraV1.getOptimalPreviewSize(mCamera.getCamera(), width, height);
                Log.d("onSurfaceChanged", "preview wh " + size.width + "," + size.height);
                mCamera.startPreview(size.width, size.height);
            }

            @Override
            public void onDrawFrame(GL10 gl) {

            }
        });

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    1324);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mView.onPause();
        mCamera.releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mView.onResume();
        mCamera.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1324) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                mCamera.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
        }
    }
}
