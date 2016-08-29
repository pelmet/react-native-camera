/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/3/16.
 */

package com.lwansbrough.RCTCamera;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

class RCTCameraViewFinder extends TextureView implements TextureView.SurfaceTextureListener, TextureView.OnTouchListener {
    private int _cameraType;
    private SurfaceTexture _surfaceTexture;
    private boolean _isStarting;
    private boolean _isStopping;
    private Camera _camera;
    private RCTSensorOrientationChecker _sensorOrientationChecker;

    public static  final int FOCUS_AREA_SIZE_MIN = 100;
    public static  final int FOCUS_AREA_SIZE_MAX = 300;
    public static  final int FOCUS_AREA_SIZE= 50;

    public RCTCameraViewFinder(Context context, int type) {
        super(context);
        this.setSurfaceTextureListener(this);
        this._cameraType = type;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        _surfaceTexture = surface;
        startCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        _surfaceTexture = null;
        stopCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

      public double getRatio() {
        int width = RCTCamera.getInstance().getPreviewWidth(this._cameraType);
        int height = RCTCamera.getInstance().getPreviewHeight(this._cameraType);

        return ((float) width) / ((float) height);
    }

    public void setCameraType(final int type) {
        if (this._cameraType == type) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                stopPreview();
                _cameraType = type;
                startPreview();
            }
        }).start();
    }

    public void setCaptureQuality(String captureQuality) {
        RCTCamera.getInstance().setCaptureQuality(_cameraType, captureQuality);
    }

    public void setTorchMode(int torchMode) {
        RCTCamera.getInstance().setTorchMode(_cameraType, torchMode);
    }

    public void setFlashMode(int flashMode) {
        RCTCamera.getInstance().setTorchMode(_cameraType, flashMode);
    }

    private void startPreview() {
        if (_surfaceTexture != null) {
            startCamera();
        }
    }

    private void stopPreview() {
        if (_camera != null) {
            stopCamera();
        }
    }

    synchronized private void startCamera() {
        if (!_isStarting) {
            _isStarting = true;

            int a = Camera.getNumberOfCameras();

            for (int i = 0; i < a; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    break;
                }
            }

            try {
                _camera = RCTCamera.getInstance().acquireCameraInstance(_cameraType);
                Camera.Parameters parameters = _camera.getParameters();
                // set autofocus
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                // set picture size
                // defaults to max available size
                Camera.Size optimalPictureSize = RCTCamera.getInstance().getBestPictureSize(_cameraType, Integer.MAX_VALUE, Integer.MAX_VALUE);
                parameters.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);

                _camera.setParameters(parameters);
                _camera.setPreviewTexture(_surfaceTexture);
                _camera.startPreview();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                stopCamera();
            } finally {
                _isStarting = false;
            }
        }
    }

    synchronized private void stopCamera() {
        if (!_isStopping) {
            _isStopping = true;
            try {
                if (_camera != null) {
                    _camera.stopPreview();
                    RCTCamera.getInstance().releaseCameraInstance(_cameraType);
                    _camera = null;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                _isStopping = false;
            }
        }
    }

    public void focusOnTouch(MotionEvent event, int width, int height) {
        if (_camera != null ) {
            Log.e("event.getX()", event.getX() + "");
            Log.e("event.getY()", event.getY() + "");

            Log.e("Finder width", width + "");
            Log.e("Finder height", height + "");

            Camera.Parameters parameters = _camera.getParameters();
            final List<String> focusModes = parameters.getSupportedFocusModes();

            Log.e("focus modes: ", focusModes.toString());


            if (parameters.getMaxNumFocusAreas() > 0){
                Rect rect = calculateFocusArea(event.getX(), event.getY(), width, height, getOrientation());

                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(rect, 900));
                parameters.setFocusAreas(meteringAreas);

                try {
                    _camera.setParameters(parameters);
                    _camera.autoFocus(mAutoFocusTakePictureCallback);
/*
                    parameters = _camera.getParameters();
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    _camera.setParameters(parameters);*/

                } catch (Exception e) {
                    Log.e("Camera err", e.getMessage());
                    e.printStackTrace();
                }
            } else {
                Log.e("Camera", "No autofocus areas");
                _camera.autoFocus(mAutoFocusTakePictureCallback);
            }
        }
    }

    public Rect calculateFocusArea(float x, float y, int width, int height, int orientation) {
        RCTCamera _cameraPreview = RCTCamera.getInstance();
        int focusAreaSize = (int) FOCUS_AREA_SIZE / 2;
        int left = clamp(Float.valueOf(2000 - (x / width) * 2000 - 1000).intValue(), focusAreaSize); // prevraceni
        int top = clamp(Float.valueOf((y / height) * 2000 - 1000).intValue(), focusAreaSize);

        Log.i("X", Float.toString(x));
        Log.i("Y", Float.toString(y));
        Log.i("LEFT", Integer.toString(left));
        Log.i("TOP", Integer.toString(top));
        Log.i("WIDTH", Integer.toString(width));
        Log.i("HEIGHT", Integer.toString(height));
        Log.e("camera orientation", Integer.toString(orientation));


        return new Rect(top, left, top + focusAreaSize / 2, left + focusAreaSize / 2);
    }

    public int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        int faSize = (int) focusAreaSize / 2;
        if ((Math.abs(touchCoordinateInCameraReper) + (faSize)) > 1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - faSize;
            } else {
                result = -1000 + faSize;
            }
        } else{
            result = touchCoordinateInCameraReper - faSize;
        }
        return result;
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    private Camera.AutoFocusCallback mAutoFocusTakePictureCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                // do something...
                Log.i("tap_to_focus", "success!");
            } else {
                // do something...
                Log.i("tap_to_focus", "fail!");
            }
        }
    };

    public void setAutofocusCallback(Camera.AutoFocusCallback callback) {
        if(_camera != null) {
            _camera.autoFocus(callback);
        }

        mAutoFocusTakePictureCallback = callback;
    }

    public int getOrientation() {
        if(_sensorOrientationChecker == null) {
            _sensorOrientationChecker = new RCTSensorOrientationChecker(getContext());
        }

        return _sensorOrientationChecker.getOrientation();
    }
}
