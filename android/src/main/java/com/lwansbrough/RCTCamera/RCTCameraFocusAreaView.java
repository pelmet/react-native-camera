package com.lwansbrough.RCTCamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

/**
 * Created by vojtaz on 14.07.16.
 */
public class RCTCameraFocusAreaView extends View {

    private Paint paint = new Paint();
    protected Boolean isFocused = false;

    public RCTCameraFocusAreaView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);

        if(isFocused) {
            paint.setColor(Color.GREEN);
        } else {
            paint.setColor(Color.GRAY);
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int piece = width / 10;

        canvas.drawRect(0, 0, 4, height, paint);
        canvas.drawRect(width - 4, 0, width, height, paint);

        canvas.drawRect(0, 0, piece, 4, paint);
        canvas.drawRect(width - piece, 0, width, 4, paint);

        canvas.drawRect(0, height - 4, piece, height, paint);
        canvas.drawRect(width - piece, height - 4, width, height, paint);
    }

    public void setFocused(Boolean bool) {
        isFocused = bool;
        invalidate();
    }
}
