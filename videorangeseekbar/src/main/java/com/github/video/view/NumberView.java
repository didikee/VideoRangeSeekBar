package com.github.video.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * user author: didikee
 * create time: 3/30/19 3:03 PM
 * description: 
 */
public class NumberView extends HalfLinearLayout {
    private ImageView minusImageView, plusImageView;
    private TextView textView;
    private View leftDivider, rightDivider;
    private OnNumberClickListener numberClickListener;

    public interface OnNumberClickListener {
        void onMinus();

        void onPlus();
    }

    public NumberView(Context context) {
        super(context);
        init(context);
    }

    public NumberView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NumberView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public NumberView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(@NonNull Context context) {
        this.setOrientation(HORIZONTAL);
        this.setGravity(Gravity.CENTER_VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.media_view_half_touch_layout, this);

        minusImageView = findViewById(R.id.minus);
        plusImageView = findViewById(R.id.plus);
        textView = findViewById(R.id.text);
        leftDivider = findViewById(R.id.left_divider);
        rightDivider = findViewById(R.id.right_divider);

        this.setOnPressedChangedListener(new OnPressedChangedListener() {
            @Override
            public void onPressedLeft(View view) {
                handleLevelBackground(view, 1);
                if (numberClickListener != null) {
                    numberClickListener.onMinus();
                }
            }

            @Override
            public void onPressedRight(View view) {
                handleLevelBackground(view, 2);
                if (numberClickListener != null) {
                    numberClickListener.onPlus();
                }
            }

            @Override
            public void onPressUp(View view) {
                handleLevelBackground(view, 0);
            }
        });
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public ImageView getMinusImageView() {
        return minusImageView;
    }

    public ImageView getPlusImageView() {
        return plusImageView;
    }

    public View getLeftDivider() {
        return leftDivider;
    }

    public View getRightDivider() {
        return rightDivider;
    }

    public TextView getTextView() {
        return textView;
    }

    public void setOnNumberClickListener(OnNumberClickListener numberClickListener) {
        this.numberClickListener = numberClickListener;
    }

    private void handleLevelBackground(@NonNull View view, int level) {
        Drawable background = view.getBackground();
        if (background instanceof LevelListDrawable) {
            background.setLevel(level);
        }
    }
}
