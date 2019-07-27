package com.github.video.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

/**
 * user author: didikee
 * create time: 3/26/19 5:44 PM
 * description: 
 */
public class HalfLinearLayout extends LinearLayout {

    private boolean mPressedLeft = false;
    private boolean mPressedRight = false;
    private OnPressedChangedListener mOnPressedChangedListener;

    public interface OnPressedChangedListener {
        void onPressedLeft(View view);

        void onPressedRight(View view);

        void onPressUp(View view);
    }

    public HalfLinearLayout(Context context) {
        super(context);
        init();
    }

    public HalfLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HalfLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public HalfLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setOnPressedChangedListener(OnPressedChangedListener mOnPressedChangedListener) {
        this.mOnPressedChangedListener = mOnPressedChangedListener;
    }

    private void init() {
        this.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mPressedLeft) {

                } else if (mPressedRight) {

                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int width = getWidth();
        float x = event.getX();
        boolean left = x < width / 2;
        if (action == MotionEvent.ACTION_DOWN) {
            if (mOnPressedChangedListener != null) {
                if (left) {
                    mPressedLeft = true;
                    mOnPressedChangedListener.onPressedLeft(this);
                } else {
                    mPressedRight = true;
                    mOnPressedChangedListener.onPressedRight(this);
                }
            }
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            if (mOnPressedChangedListener != null) {
                mPressedRight = false;
                mPressedLeft = false;
                mOnPressedChangedListener.onPressUp(this);
            }
        }
        return true;
    }


}
