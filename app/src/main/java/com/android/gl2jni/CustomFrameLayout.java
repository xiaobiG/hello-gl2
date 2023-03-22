package com.android.gl2jni;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class CustomFrameLayout extends FrameLayout {
    private float mAspectRatio = 1.0f;

    public CustomFrameLayout(Context context) {
        super(context);
    }

    public CustomFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
//        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomFrameLayout);
//        mAspectRatio = a.getFloat(R.styleable.CustomFrameLayout_aspectRatio, 1.0f);
//        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (mAspectRatio != 0.0f) {
            // Calculate the height based on the width and the aspect ratio
            int calculatedHeight = (int) (widthSize / mAspectRatio + 0.5f);

            // If the calculated height is smaller than the available height,
            // use it instead of the available height
            if (calculatedHeight < heightSize) {
                heightSize = calculatedHeight;
            }
        }

        // Call the superclass onMeasure with the modified dimensions
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
    }

    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
        requestLayout();
    }
}
