package com.github.video.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * 这个设计来自tg，然后被重构了
 * 目前针对视频裁剪，这个view只做三个功能
 * 1. 左滑块控制裁剪开始的时间
 * 2. 右滑块控制裁剪结束的时间
 * 3. 中的指针代表的是播放进度。
 * 当正在播放时自动移动代表进度。（此时无法点击）
 * 当暂停播放的时候拖动中间的滑块可以预览。
 *
 */
@TargetApi(10)
public class VideoRangeSeekBar extends View {

    private long videoLength;
    private float progressLeft = 0f;
    private float progressRight = 1f;
    private Paint whitePaint;
    private Paint shadowPaint;
    private boolean pressedLeft;
    private boolean pressedRight;
    private boolean pressedPlay;
    private float playProgress = 0f;
    private float pressDx;
    private FrameHandler frameHandler;
    //    private MediaMetadataRetriever mediaMetadataRetriever;
    private VideoRangeSeekBarListener delegate;
    private ArrayList<Bitmap> frames = new ArrayList<>();
    private AsyncTask<Integer, Integer, Bitmap> currentTask;
    private static final Object sync = new Object();
    private long frameTimeOffset;
    private int frameWidth;
    private int frameHeight;
    private int framesToLoad;
    private float maxProgressDiff = 1.0f;
    private float minProgressDiff = 0.0f;
    private boolean isRoundFrames;
    private Rect rect1;
    private Rect rect2;
    private RectF rect3 = new RectF();
    private Drawable drawableLeft;
    private Drawable drawableRight;
    private int lastWidth;
    public float density = 1;
    //一些间距
    private int bgPaddingLeft, bgPaddingRight;// 背景的间距
    private int viewHeight;//view的高度，被tg写死了
    private int centerLinePaddingTop;//2dp
    private int lineTopPadding;//上下两条横线的位置
    private int radius;//画圆角的半径
    private boolean mVideoPlaying = false;//视频是否在播放

    public interface VideoRangeSeekBarListener {
        void onLeftProgressChanged(float leftProgress);

        void onRightProgressChanged(float rightProgress);

        void onPlayProgressChanged(float progress);

        void didStartDragging();

        void didStopDragging();
    }

    public VideoRangeSeekBar(Context context) {
        super(context);
        init(context);
    }

    public VideoRangeSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoRangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VideoRangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setColor(0xffffffff);
        shadowPaint = new Paint();
        shadowPaint.setColor(0x7f000000);
        drawableLeft = context.getResources().getDrawable(R.drawable.video_cropleft);
        drawableLeft.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));
        drawableRight = context.getResources().getDrawable(R.drawable.video_cropright);
        drawableRight.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));

        bgPaddingLeft = bgPaddingRight = dp(16);
        lineTopPadding = dp(4);
        centerLinePaddingTop = dp(2);
        radius = dp(2);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();

        int width = getMeasuredWidth() - dp(32);
        int startX = (int) (width * progressLeft) + bgPaddingLeft;
        int playX = (int) (width * playProgress + bgPaddingLeft);
//        int playX = (int) (width * (progressLeft + (progressRight - progressLeft) * playProgress)) + dp(16);
        int endX = (int) (width * progressRight) + bgPaddingRight;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (!isFrameHandlerEnable()) {
                return false;
            }
            int additionWidth = dp(12);//默认是12
            int additionWidthPlay = dp(8);
            if (y >= 0 && y <= getMeasuredHeight()) {
                float startDistance = Math.abs(x - startX);
                float playDistance = Math.abs(x - playX);
                float endDistance = Math.abs(x - endX);
                float minDistance = Math.min(Math.min(startDistance, playDistance), endDistance);
                if (Math.abs(minDistance) >= additionWidth) {
                    return false;
                }

                if (endX == startX) {
                    // play 不参与
                    float offsetX = x - startX;
                    if (offsetX > 0) {
                        //right
                        pressedRight = true;
                    } else {
                        pressedLeft = true;
                    }
                } else {
                    boolean playEnable = endX - startX > additionWidthPlay;
                    // play 参与
                    if (minDistance == startDistance && minDistance == playDistance && playEnable) {
                        float offsetX = x - startX;
                        if (offsetX > 0) {
                            //right
                            pressedPlay = true;
                        } else {
                            pressedLeft = true;
                        }
                    } else if (minDistance == endDistance && minDistance == playDistance && playEnable) {
                        float offsetX = x - endX;
                        if (offsetX > 0) {
                            //right
                            pressedRight = true;
                        } else {
                            pressedPlay = true;
                        }
                    } else {
                        if (minDistance == startDistance) {
                            pressedLeft = true;
                        } else if (minDistance == endDistance) {
                            pressedRight = true;
                        } else {
                            if (minDistance < additionWidthPlay && playEnable) {
                                pressedPlay = true;
                            }
                        }
                    }
                }
                if (pressedLeft) {
                    if (delegate != null) {
                        delegate.didStartDragging();
                    }
                    log("pressedLeft");
                    pressDx = (int) (x - startX);
                    invalidate();
                    return true;
                }
                if (pressedRight) {
                    if (delegate != null) {
                        delegate.didStartDragging();
                    }
                    log("pressedRight");
                    pressDx = (int) (x - endX);
                    invalidate();
                    return true;
                }
                if (pressedPlay) {
                    if (delegate != null) {
                        delegate.didStartDragging();
                    }
                    log("pressedPlay");
                    pressDx = (int) (x - playX);
                    invalidate();
                    return true;
                }

//                if (minDistance == startDistance) {
//                    if (delegate != null) {
//                        delegate.didStartDragging();
//                    }
//                    log("pressedLeft");
//                    pressedLeft = true;
//                    pressDx = (int) (x - startX);
//                    invalidate();
//                    return true;
//                } else if (minDistance == endDistance) {
//                    if (delegate != null) {
//                        delegate.didStartDragging();
//                    }
//                    log("pressedRight");
//                    pressedRight = true;
//                    pressDx = (int) (x - endX);
//                    invalidate();
//                    return true;
//                } else {
//                    if (minDistance < additionWidthPlay) {
//                        if (delegate != null) {
//                            delegate.didStartDragging();
//                        }
//                        log("pressedPlay");
//                        pressedPlay = true;
//                        pressDx = (int) (x - playX);
//                        invalidate();
//                        return true;
//                    }
//                }

//                if (x <= startX + additionWidth) {
//                    if (delegate != null) {
//                        delegate.didStartDragging();
//                    }
//                    log("pressedLeft");
//                    pressedLeft = true;
//                    pressDx = (int) (x - startX);
//                    invalidate();
//                    return true;
//                }
//                if (x >= endX - additionWidth) {
//                    if (delegate != null) {
//                        delegate.didStartDragging();
//                    }
//                    log("pressedRight");
//                    pressedRight = true;
//                    pressDx = (int) (x - endX);
//                    invalidate();
//                    return true;
//                }
//                if (playX - additionWidthPlay <= x && x <= playX + additionWidthPlay) {
//                    if (delegate != null) {
//                        delegate.didStartDragging();
//                    }
//                    log("pressedPlay");
//                    pressedPlay = true;
//                    pressDx = (int) (x - playX);
//                    invalidate();
//                    return true;
//                }
            }
//            if (startX - additionWidth <= x && x <= startX + additionWidth && y >= 0 && y <= getMeasuredHeight()) {
//                if (delegate != null) {
//                    delegate.didStartDragging();
//                }
//                log("pressedLeft");
//                pressedLeft = true;
//                pressDx = (int) (x - startX);
//                invalidate();
//                return true;
//            } else if (endX - additionWidth <= x && x <= endX + additionWidth && y >= 0 && y <= getMeasuredHeight()) {
//                if (delegate != null) {
//                    delegate.didStartDragging();
//                }
//                log("pressedRight");
//                pressedRight = true;
//                pressDx = (int) (x - endX);
//                invalidate();
//                return true;
//            } else if (playX - additionWidthPlay <= x && x <= playX + additionWidthPlay && y >= 0 && y <= getMeasuredHeight()) {
//                if (delegate != null) {
//                    delegate.didStartDragging();
//                }
//                log("pressedPlay");
//                pressedPlay = true;
//                pressDx = (int) (x - playX);
//                invalidate();
//                return true;
//            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            pressedLeft = false;
            pressedRight = false;
            pressedPlay = false;
            if (delegate != null) {
                delegate.didStopDragging();
            }
            return true;
//            if (pressedLeft) {
//                if (delegate != null) {
//                    delegate.didStopDragging();
//                }
//                pressedLeft = false;
//                return true;
//            } else if (pressedRight) {
//                if (delegate != null) {
//                    delegate.didStopDragging();
//                }
//                pressedRight = false;
//                return true;
//            } else if (pressedPlay) {
//                if (delegate != null) {
//                    delegate.didStopDragging();
//                }
//                pressedPlay = false;
//                return true;
//            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (pressedPlay) {
                playX = (int) (x - pressDx);
                playProgress = (float) (playX - dp(16)) / (float) width;
//                playProgress = (playProgress - progressLeft) / (progressRight - progressLeft);
                if (delegate != null) {
                    delegate.onPlayProgressChanged(playProgress);
                }
                invalidate();
                return true;
            } else if (pressedLeft) {
                startX = (int) (x - pressDx);
                if (startX < bgPaddingLeft) {
                    startX = bgPaddingLeft;
                } else if (startX > endX) {
                    startX = endX;
                }
                progressLeft = (float) (startX - bgPaddingLeft) / (float) width;
                if (progressRight - progressLeft > maxProgressDiff) {
                    progressRight = progressLeft + maxProgressDiff;
                } else if (minProgressDiff != 0 && progressRight - progressLeft < minProgressDiff) {
                    progressLeft = progressRight - minProgressDiff;
                    if (progressLeft < 0) {
                        progressLeft = 0;
                    }
                }
                if (delegate != null) {
                    delegate.onLeftProgressChanged(progressLeft);
                }
                invalidate();
                return true;
            } else if (pressedRight) {
                endX = (int) (x - pressDx);
                if (endX < startX) {
                    endX = startX;
                } else if (endX > width + bgPaddingRight) {
                    endX = width + bgPaddingRight;
                }
                progressRight = (float) (endX - bgPaddingRight) / (float) width;
                if (progressRight - progressLeft > maxProgressDiff) {
                    progressLeft = progressRight - maxProgressDiff;
                } else if (minProgressDiff != 0 && progressRight - progressLeft < minProgressDiff) {
                    progressRight = progressLeft + minProgressDiff;
                    if (progressRight > 1.0f) {
                        progressRight = 1.0f;
                    }
                }
                if (delegate != null) {
                    delegate.onRightProgressChanged(progressRight);
                }
                invalidate();
                return true;
            }
        }
        return false;
    }


    public float getPlayProgress() {
        return playProgress;
    }

    public float getLeftProgress() {
        return progressLeft;
    }

    public float getRightProgress() {
        return progressRight;
    }

    public void setPlayProgress(float value) {
        playProgress = value;
        invalidate();
    }

    public void setRightProgress(float progressRight) {
        this.progressRight = progressRight;
        invalidate();
    }

    public void setLeftProgress(float progressLeft) {
        this.progressLeft = progressLeft;
        invalidate();
    }

    public void setMinProgressDiff(float value) {
        minProgressDiff = value;
    }

    public void setMaxProgressDiff(float value) {
        maxProgressDiff = value;
        if (progressRight - progressLeft > maxProgressDiff) {
            progressRight = progressLeft + maxProgressDiff;
            invalidate();
        }
    }

    public void setRoundFrames(boolean value) {
        isRoundFrames = value;
        if (isRoundFrames) {
            rect1 = new Rect(dp(14), dp(14), dp(14 + 28), dp(14 + 28));
            rect2 = new Rect();
        }
    }

    /**
     * 当视频播放时无法拖动进度条
     * @param playing
     */
    public void setVideoPlaying(boolean playing) {
        this.mVideoPlaying = playing;
    }

    public void setColor(int color) {
        whitePaint.setColor(color);
    }

    public void startWithHandler(FrameHandler handler) {
        this.frameHandler = handler;
        destroy();
        progressLeft = 0.0f;
        progressRight = 1.0f;
        invalidate();
    }

//    public void setVideoPath(String path) {
//        destroy();
//        mediaMetadataRetriever = new MediaMetadataRetriever();
//        progressLeft = 0.0f;
//        progressRight = 1.0f;
//        try {
//            mediaMetadataRetriever.setDataSource(path);
//            String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
//            videoLength = Long.parseLong(duration);
//        } catch (Exception e) {
//        }
//        invalidate();
//    }

//    public void setVideoSource(Context context, Uri videoUri) {
//        destroy();
//        mediaMetadataRetriever = new MediaMetadataRetriever();
//        progressLeft = 0.0f;
//        progressRight = 1.0f;
//        try {
//            mediaMetadataRetriever.setDataSource(context, videoUri);
//            String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
//            videoLength = Long.parseLong(duration);
//        } catch (Exception e) {
//        }
//        invalidate();
//    }

    public void setOnVideoRangeSeekBarListener(VideoRangeSeekBarListener delegate) {
        this.delegate = delegate;
    }

    public void checkPlayProgress() {
        if (playProgress < progressLeft) {
            playProgress = progressLeft;
        } else if (playProgress > progressRight) {
            playProgress = progressRight;
        }
    }

    public FrameHandler getFrameHandler() {
        return frameHandler;
    }

    // 判断frame handler 是否正常可用
    public boolean isFrameHandlerEnable() {
        return frameHandler != null && frameHandler.isAvailable();
    }

    private void reloadFrames(int frameNum) {
        if (!isFrameHandlerEnable()) {
            return;
        }
        loadFrames(frameNum);
    }

    protected void loadFrames(int frameNum) {
        if (frameNum == 0) {
            if (isRoundFrames) {
                frameHeight = frameWidth = dp(56);
                framesToLoad = (int) Math.ceil((getMeasuredWidth() - dp(16)) / (frameHeight / 2.0f));
            } else {
                frameHeight = getShadowBgHeight();
                framesToLoad = (getMeasuredWidth() - dp(16)) / frameHeight;
                frameWidth = (int) Math.ceil((float) (getMeasuredWidth() - dp(16)) / (float) framesToLoad);
            }
            frameTimeOffset = videoLength / framesToLoad;
        }
        currentTask = new AsyncTask<Integer, Integer, Bitmap>() {
            private int frameNum = 0;

            @Override
            protected Bitmap doInBackground(Integer... objects) {
                frameNum = objects[0];
                Bitmap bitmap = null;
                if (isCancelled()) {
                    return null;
                }
                try {
                    bitmap = frameHandler.getFrameAtTime(frameTimeOffset * frameNum);
                    if (isCancelled()) {
                        return null;
                    }
                    if (bitmap != null) {
                        Bitmap result = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.getConfig());
                        Canvas canvas = new Canvas(result);
                        float scaleX = (float) frameWidth / (float) bitmap.getWidth();
                        float scaleY = (float) frameHeight / (float) bitmap.getHeight();
                        float scale = scaleX > scaleY ? scaleX : scaleY;
                        int w = (int) (bitmap.getWidth() * scale);
                        int h = (int) (bitmap.getHeight() * scale);
                        Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        Rect destRect = new Rect((frameWidth - w) / 2, (frameHeight - h) / 2, w, h);
                        canvas.drawBitmap(bitmap, srcRect, destRect, null);
                        bitmap.recycle();
                        bitmap = result;
                    }
                } catch (Exception e) {
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (!isCancelled()) {
                    frames.add(bitmap);
                    invalidate();
                    if (frameNum < framesToLoad) {
                        reloadFrames(frameNum + 1);
                    }
                }
            }
        };
        currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, frameNum, null, null);
    }

    public void destroy() {
        if (frameHandler != null) {
            frameHandler.onDestroy();
        }
        for (Bitmap bitmap : frames) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        frames.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    public boolean isDragging() {
        return pressedPlay;
    }

    public void clearFrames() {
        for (Bitmap bitmap : frames) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        frames.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        invalidate();
    }

    private int getMySize(int defaultSize, int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        if (mode == MeasureSpec.UNSPECIFIED) {
            //如果没有指定大小，就设置为默认大小
            return defaultSize;
        } else if (mode == MeasureSpec.AT_MOST) {
            //如果测量模式是最大取值为size
            //我们将大小取最大值,你也可以取其他值
            return Math.max(size, defaultSize);
        } else if (mode == MeasureSpec.EXACTLY) {
            //如果是固定的大小，那就不要去改变它
            return Math.max(size, defaultSize);
        }
        return defaultSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int defaultHeight = dp(64);

        viewHeight = getMySize(defaultHeight, heightMeasureSpec);
        if (lastWidth != widthSize) {
            clearFrames();
            lastWidth = widthSize;
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        // bg padding = 16dp
        int width = getMeasuredWidth() - dp(36);
        int startX = (int) (width * progressLeft) + bgPaddingLeft;
        int endX = (int) (width * progressRight) + bgPaddingRight;

        int lineThickness = dp(2);
        // 以阴影的上边界为准，4位padding，2是line的厚度
        int top = lineTopPadding + lineThickness;// 4+2
        int end = viewHeight - dp(8);

        canvas.save();
        // 裁剪画布
        canvas.clipRect(dp(16), dp(4), width + dp(20), (end));
        if (frames.isEmpty() && currentTask == null) {
            reloadFrames(0);
        } else {
            int offset = 0;
            for (int a = 0; a < frames.size(); a++) {
                Bitmap bitmap = frames.get(a);
                if (bitmap != null) {
                    int x = dp(16) + offset * (isRoundFrames ? frameWidth / 2 : frameWidth);
                    int y = dp(2 + 4);
                    if (isRoundFrames) {
                        rect2.set(x, y, x + dp(28), y + dp(28));
                        canvas.drawBitmap(bitmap, rect1, rect2, null);
                    } else {
                        canvas.drawBitmap(bitmap, x, y, null);
                    }
                }
                offset++;
            }
        }


        int bgOffset = dp(2);
        int thumbWidth = dp(12);

        // 左侧的阴影
        canvas.drawRect(bgPaddingLeft, top, startX, (end - lineThickness), shadowPaint);
        // 右侧的阴影
        canvas.drawRect(endX + dp(4), top, bgPaddingRight + width + dp(4), (end - lineThickness), shadowPaint);

        // 画上下横线
        // 画垂直辅助线-左（让圆角包裹的更好）
        canvas.drawRect(startX, dp(4), startX + bgOffset, end, whitePaint);
        // 画垂直辅助线-右（让圆角包裹的更好）
        canvas.drawRect(endX + bgOffset, dp(4), endX + bgOffset * 2, end, whitePaint);

        canvas.drawRect(startX + bgOffset, top - lineThickness, endX + bgOffset * 2, top, whitePaint);
        canvas.drawRect(startX + bgOffset, end - lineThickness, endX + bgOffset * 2, end, whitePaint);
        canvas.restore();

        int thumbIconHeight = dp(18);
        int thumbTopLocation = (top - lineThickness) + ((end - (top - lineThickness) - thumbIconHeight) / 2);
        int thumbBottomLocation = (top - lineThickness) + ((end - (top - lineThickness) + thumbIconHeight) / 2);
        // 左边的thumb
        rect3.set(startX - (thumbWidth - bgOffset), (top - lineThickness), startX + bgOffset, end);
        canvas.drawRoundRect(rect3, radius, radius, whitePaint);
        int thumbOffset = dp(2);
        drawableLeft.setBounds(startX - (thumbWidth - thumbOffset),
                thumbTopLocation,
                startX + thumbOffset,
                thumbBottomLocation);
        drawableLeft.draw(canvas);

        // 右边的thumb
        rect3.set(endX + bgOffset, (top - lineThickness), endX + (bgOffset + thumbWidth), end);
        canvas.drawRoundRect(rect3, radius, radius, whitePaint);
        drawableRight.setBounds(endX + thumbOffset,
                thumbTopLocation,
                endX + (thumbWidth + thumbOffset),
                thumbBottomLocation);
        drawableRight.draw(canvas);
        int smallRadius = dp(1);

        // 画中间的play 标杆
//        float cx = dp(18) + width * (progressLeft + (progressRight - progressLeft) * playProgress);
        float cx = width * playProgress + bgPaddingLeft + dp(1);
//        rect3.set(cx - dp(1.5f), dp(2), cx + dp(1.5f), dp(50));
//        canvas.drawRoundRect(rect3, smallRadius, smallRadius, shadowPaint);
//        canvas.drawCircle(cx, dp(52), dp(3.5f), shadowPaint);

        int bigRadius = dp(3);
        // 画中间的play 圆形
        rect3.set(cx - dp(1), centerLinePaddingTop/*2dp*/, cx + dp(1), end + lineThickness);
        canvas.drawRoundRect(rect3, smallRadius, smallRadius, whitePaint);
        canvas.drawCircle(cx, viewHeight - bigRadius - 1, bigRadius, whitePaint);
    }

    private int getShadowBgHeight() {
        int lineThickness = getLineThickness();
        int top = getTopLayer();
        int end = viewHeight - dp(8);
        return (end - lineThickness) - top;
    }

    private int getTopLayer() {
        return dp(2 + 4);
    }

    private int getEndLayer() {
        return dp(2 + 4);
    }

    private int getLineThickness() {
        return dp(2);
    }

    private void log(String message) {
        Log.d("VideoTimeLine", message);
    }

    public int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }
}
