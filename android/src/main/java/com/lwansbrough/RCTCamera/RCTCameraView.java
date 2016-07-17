/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/3/16.
 */

package com.lwansbrough.RCTCamera;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.*;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RCTCameraView extends ViewGroup {
    private final OrientationEventListener _orientationListener;
    private final Context _context;
    private RCTCameraViewFinder _viewFinder = null;
    private int _actualDeviceOrientation = -1;
    private int _aspect = RCTCameraModule.RCT_CAMERA_ASPECT_FIT;
    private String _captureQuality = "high";
    private int _torchMode = -1;
    private int _flashMode = -1;

    protected RCTCameraFocusAreaView _focusArea;

    public RCTCameraView(Context context) {
        super(context);
        this._context = context;
        setActualDeviceOrientation(context);

        _orientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (setActualDeviceOrientation(_context)) {
                    layoutViewFinder();
                }
            }
        };

        if (_orientationListener.canDetectOrientation()) {
            _orientationListener.enable();
        } else {
            _orientationListener.disable();
        }

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                _focusArea.setFocused(false);
                _viewFinder.focusOnTouch(event, getWidth(), getHeight());
                layoutFocusArea(event);
                return false;
            }
        });

        _focusArea = new RCTCameraFocusAreaView(_context);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutViewFinder(left, top, right, bottom);
    }

    @Override
    public void onViewAdded(View child) {
        if (this._viewFinder == child) return;
        // remove and readd view to make sure it is in the back.
        // @TODO figure out why there was a z order issue in the first place and fix accordingly.
        this.removeView(this._viewFinder);
        this.addView(this._viewFinder, 0);

        bringChildToFront(_focusArea);
    }

    public void setAspect(int aspect) {
        this._aspect = aspect;
        layoutViewFinder();
    }

    public void setCameraType(final int type) {
        if (null != this._viewFinder) {
            this._viewFinder.setCameraType(type);
        } else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                _viewFinder = new RCTCameraViewFinder2(_context, type);
            } else {
                _viewFinder = new RCTCameraViewFinder(_context, type);
            }

            if (-1 != this._flashMode) {
                _viewFinder.setFlashMode(this._flashMode);
            }
            if (-1 != this._torchMode) {
                _viewFinder.setFlashMode(this._torchMode);
            }

            _viewFinder.setAutofocusCallback(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if(success) {
                        _focusArea.setFocused(true);
                    }
                }
            });

            addView(_viewFinder);
            addView(_focusArea);
        }
    }

    public void setCaptureQuality(String captureQuality) {
        this._captureQuality = captureQuality;
        if (this._viewFinder != null) {
            this._viewFinder.setCaptureQuality(captureQuality);
        }
    }

    public void setTorchMode(int torchMode) {
        this._torchMode = torchMode;
        if (this._viewFinder != null) {
            this._viewFinder.setTorchMode(torchMode);
        }
    }

    public void setFlashMode(int flashMode) {
        this._flashMode = flashMode;
        if (this._viewFinder != null) {
            this._viewFinder.setFlashMode(flashMode);
        }
    }

    public void setOrientation(int orientation) {
        RCTCamera.getInstance().setOrientation(orientation);
        if (this._viewFinder != null) {
            layoutViewFinder();
        }
    }

    private boolean setActualDeviceOrientation(Context context) {
        int actualDeviceOrientation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
        if (_actualDeviceOrientation != actualDeviceOrientation) {
            _actualDeviceOrientation = actualDeviceOrientation;
            RCTCamera.getInstance().setActualDeviceOrientation(_actualDeviceOrientation);
            return true;
        } else {
            return false;
        }
    }

    private void layoutViewFinder() {
        layoutViewFinder(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
    }

    protected void layoutFocusArea(int x, int y) {
        int focusAreaSize = RCTCameraViewFinder.FOCUS_AREA_SIZE;
        int focusAreaSizeHalf = focusAreaSize / 2;
        int t = x - focusAreaSizeHalf;
        int l =  y - focusAreaSizeHalf;

        _focusArea.layout(t, l, t + focusAreaSize, l + focusAreaSize);
    }

    protected void layoutFocusArea(MotionEvent event) {
        layoutFocusArea((int) event.getX(), (int) event.getY());
    }

    private void layoutViewFinder(int left, int top, int right, int bottom) {
        if (null == _viewFinder) {
            return;
        }
        float width = right - left;
        float height = bottom - top;
        int viewfinderWidth;
        int viewfinderHeight;
        double ratio;

        layoutFocusArea((int) (width / 2), (int) (height / 2));

        switch (this._aspect) {
            case RCTCameraModule.RCT_CAMERA_ASPECT_FIT:
                ratio = this._viewFinder.getRatio();
                if (ratio * height > width) {
                    viewfinderHeight = (int) (width / ratio);
                    viewfinderWidth = (int) width;
                } else {
                    viewfinderWidth = (int) (ratio * height);
                    viewfinderHeight = (int) height;
                }
                break;
            case RCTCameraModule.RCT_CAMERA_ASPECT_FILL:
                ratio = this._viewFinder.getRatio();
                if (ratio * height < width) {
                    viewfinderHeight = (int) (width / ratio);
                    viewfinderWidth = (int) width;
                } else {
                    viewfinderWidth = (int) (ratio * height);
                    viewfinderHeight = (int) height;
                }
                break;
            default:
                viewfinderWidth = (int) width;
                viewfinderHeight = (int) height;
        }

        int viewFinderPaddingX = (int) ((width - viewfinderWidth) / 2);
        int viewFinderPaddingY = (int) ((height - viewfinderHeight) / 2);

        this._viewFinder.layout(viewFinderPaddingX, viewFinderPaddingY, viewFinderPaddingX + viewfinderWidth, viewFinderPaddingY + viewfinderHeight);
        this.postInvalidate(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
    }

}
