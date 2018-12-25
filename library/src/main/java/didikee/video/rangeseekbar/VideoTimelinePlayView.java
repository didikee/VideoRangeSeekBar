/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package didikee.video.rangeseekbar;

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
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

@TargetApi(10)
public class VideoTimelinePlayView extends View {

    private long videoLength;
    private float progressLeft;
    private float progressRight = 1;
    private Paint paint;
    private Paint shadowPaint;
    private boolean pressedLeft;
    private boolean pressedRight;
    private boolean pressedPlay;
    private float playProgress = 0.5f;
    private float pressDx;
    private MediaMetadataRetriever mediaMetadataRetriever;
    private VideoTimelineViewDelegate delegate;
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

    //一些间距
    private int bgPaddingLeft, bgPaddingRight;// 背景的间距
    private int viewHeight;//view的高度，被tg写死了

    public interface VideoTimelineViewDelegate {
        void onLeftProgressChanged(float progress);

        void onRightProgressChanged(float progress);

        void onPlayProgressChanged(float progress);

        void didStartDragging();

        void didStopDragging();
    }

    public VideoTimelinePlayView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xffffffff);
        shadowPaint = new Paint();
        shadowPaint.setColor(0x7f000000);
        drawableLeft = context.getResources().getDrawable(R.drawable.video_cropleft);
        drawableLeft.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));
        drawableRight = context.getResources().getDrawable(R.drawable.video_cropright);
        drawableRight.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));

        bgPaddingLeft = bgPaddingRight = AndroidUtilities.dp(16);
        viewHeight = AndroidUtilities.dp(56);
    }

    public float getProgress() {
        return playProgress;
    }

    public float getLeftProgress() {
        return progressLeft;
    }

    public float getRightProgress() {
        return progressRight;
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
            rect1 = new Rect(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14 + 28), AndroidUtilities.dp(14 + 28));
            rect2 = new Rect();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();

        int width = getMeasuredWidth() - AndroidUtilities.dp(32);
        int startX = (int) (width * progressLeft) + bgPaddingLeft;
        int playX = (int) (width * (progressLeft + (progressRight - progressLeft) * playProgress)) + AndroidUtilities.dp(16);
        int endX = (int) (width * progressRight) + bgPaddingRight;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (mediaMetadataRetriever == null) {
                return false;
            }
            int additionWidth = AndroidUtilities.dp(12);//默认是12
            int additionWidthPlay = AndroidUtilities.dp(8);
            if (y >= 0 && y <= getMeasuredHeight()) {
                if (x <= startX + additionWidth) {
                    if (delegate != null) {
                        delegate.didStartDragging();
                    }
                    log("pressedLeft");
                    pressedLeft = true;
                    pressDx = (int) (x - startX);
                    invalidate();
                    return true;
                }
                if (x >= endX - additionWidth) {
                    if (delegate != null) {
                        delegate.didStartDragging();
                    }
                    log("pressedRight");
                    pressedRight = true;
                    pressDx = (int) (x - endX);
                    invalidate();
                    return true;
                }
                if (playX - additionWidthPlay <= x && x <= playX + additionWidthPlay) {
                    if (delegate != null) {
                        delegate.didStartDragging();
                    }
                    log("pressedPlay");
                    pressedPlay = true;
                    pressDx = (int) (x - playX);
                    invalidate();
                    return true;
                }
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
            if (pressedLeft) {
                if (delegate != null) {
                    delegate.didStopDragging();
                }
                pressedLeft = false;
                return true;
            } else if (pressedRight) {
                if (delegate != null) {
                    delegate.didStopDragging();
                }
                pressedRight = false;
                return true;
            } else if (pressedPlay) {
                if (delegate != null) {
                    delegate.didStopDragging();
                }
                pressedPlay = false;
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (pressedPlay) {
                playX = (int) (x - pressDx);
                playProgress = (float) (playX - AndroidUtilities.dp(16)) / (float) width;
                if (playProgress < progressLeft) {
                    playProgress = progressLeft;
                } else if (playProgress > progressRight) {
                    playProgress = progressRight;
                }
                playProgress = (playProgress - progressLeft) / (progressRight - progressLeft);
                if (delegate != null) {
                    delegate.onPlayProgressChanged(progressLeft + (progressRight - progressLeft) * playProgress);
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

    public void setColor(int color) {
        paint.setColor(color);
    }

    public void setVideoPath(String path) {
        destroy();
        mediaMetadataRetriever = new MediaMetadataRetriever();
        progressLeft = 0.0f;
        progressRight = 1.0f;
        try {
            mediaMetadataRetriever.setDataSource(path);
            String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            videoLength = Long.parseLong(duration);
        } catch (Exception e) {
            FileLog.e(e);
        }
        invalidate();
    }

    public void setDelegate(VideoTimelineViewDelegate delegate) {
        this.delegate = delegate;
    }

    private void reloadFrames(int frameNum) {
        if (mediaMetadataRetriever == null) {
            return;
        }
        if (frameNum == 0) {
            if (isRoundFrames) {
                frameHeight = frameWidth = AndroidUtilities.dp(56);
                framesToLoad = (int) Math.ceil((getMeasuredWidth() - AndroidUtilities.dp(16)) / (frameHeight / 2.0f));
            } else {
                frameHeight = AndroidUtilities.dp(40);
                framesToLoad = (getMeasuredWidth() - AndroidUtilities.dp(16)) / frameHeight;
                frameWidth = (int) Math.ceil((float) (getMeasuredWidth() - AndroidUtilities.dp(16)) / (float) framesToLoad);
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
                    bitmap = mediaMetadataRetriever.getFrameAtTime(frameTimeOffset * frameNum * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
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
                    FileLog.e(e);
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
        synchronized (sync) {
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                    mediaMetadataRetriever = null;
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
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

    public void setProgress(float value) {
        playProgress = value;
        invalidate();
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (lastWidth != widthSize) {
            clearFrames();
            lastWidth = widthSize;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // bg padding = 16dp
        int width = getMeasuredWidth() - AndroidUtilities.dp(36);
        int startX = (int) (width * progressLeft) + bgPaddingLeft;
        int endX = (int) (width * progressRight) + bgPaddingRight;

        canvas.save();
        canvas.clipRect(AndroidUtilities.dp(16), AndroidUtilities.dp(4), width + AndroidUtilities.dp(20), AndroidUtilities.dp(48));
        if (frames.isEmpty() && currentTask == null) {
            reloadFrames(0);
        } else {
            int offset = 0;
            for (int a = 0; a < frames.size(); a++) {
                Bitmap bitmap = frames.get(a);
                if (bitmap != null) {
                    int x = AndroidUtilities.dp(16) + offset * (isRoundFrames ? frameWidth / 2 : frameWidth);
                    int y = AndroidUtilities.dp(2 + 4);
                    if (isRoundFrames) {
                        rect2.set(x, y, x + AndroidUtilities.dp(28), y + AndroidUtilities.dp(28));
                        canvas.drawBitmap(bitmap, rect1, rect2, null);
                    } else {
                        canvas.drawBitmap(bitmap, x, y, null);
                    }
                }
                offset++;
            }
        }

        int top = AndroidUtilities.dp(2 + 4);
        int end = AndroidUtilities.dp(48);

        canvas.drawRect(bgPaddingLeft, top, startX, AndroidUtilities.dp(46), shadowPaint);
        canvas.drawRect(endX + AndroidUtilities.dp(4), top, AndroidUtilities.dp(16) + width + AndroidUtilities.dp(4), AndroidUtilities.dp(46), shadowPaint);

        canvas.drawRect(startX, AndroidUtilities.dp(4), startX + AndroidUtilities.dp(2), end, paint);
        canvas.drawRect(endX + AndroidUtilities.dp(2), AndroidUtilities.dp(4), endX + AndroidUtilities.dp(4), end, paint);
        canvas.drawRect(startX + AndroidUtilities.dp(2), AndroidUtilities.dp(4), endX + AndroidUtilities.dp(4), top, paint);
        canvas.drawRect(startX + AndroidUtilities.dp(2), end - AndroidUtilities.dp(2), endX + AndroidUtilities.dp(4), end, paint);
        canvas.restore();

        rect3.set(startX - AndroidUtilities.dp(8), AndroidUtilities.dp(4), startX + AndroidUtilities.dp(2), end);
        canvas.drawRoundRect(rect3, AndroidUtilities.dp(2), AndroidUtilities.dp(2), paint);
        drawableLeft.setBounds(startX - AndroidUtilities.dp(8), AndroidUtilities.dp(4) + (AndroidUtilities.dp(44) - AndroidUtilities.dp(18)) / 2, startX + AndroidUtilities.dp(2), (AndroidUtilities.dp(44) - AndroidUtilities.dp(18)) / 2 + AndroidUtilities.dp(18 + 4));
        drawableLeft.draw(canvas);

        rect3.set(endX + AndroidUtilities.dp(2), AndroidUtilities.dp(4), endX + AndroidUtilities.dp(12), end);
        canvas.drawRoundRect(rect3, AndroidUtilities.dp(2), AndroidUtilities.dp(2), paint);
        drawableRight.setBounds(endX + AndroidUtilities.dp(2), AndroidUtilities.dp(4) + (AndroidUtilities.dp(44) - AndroidUtilities.dp(18)) / 2, endX + AndroidUtilities.dp(12), (AndroidUtilities.dp(44) - AndroidUtilities.dp(18)) / 2 + AndroidUtilities.dp(18 + 4));
        drawableRight.draw(canvas);

        // 画中间的play 标杆
        float cx = AndroidUtilities.dp(18) + width * (progressLeft + (progressRight - progressLeft) * playProgress);
        rect3.set(cx - AndroidUtilities.dp(1.5f), AndroidUtilities.dp(2), cx + AndroidUtilities.dp(1.5f), AndroidUtilities.dp(50));
        canvas.drawRoundRect(rect3, AndroidUtilities.dp(1), AndroidUtilities.dp(1), shadowPaint);
        canvas.drawCircle(cx, AndroidUtilities.dp(52), AndroidUtilities.dp(3.5f), shadowPaint);

        // 画中间的play 圆形
        rect3.set(cx - AndroidUtilities.dp(1), AndroidUtilities.dp(2), cx + AndroidUtilities.dp(1), AndroidUtilities.dp(50));
        canvas.drawRoundRect(rect3, AndroidUtilities.dp(1), AndroidUtilities.dp(1), paint);
        canvas.drawCircle(cx, AndroidUtilities.dp(52), AndroidUtilities.dp(3), paint);
    }

    private void log(String message) {
        Log.d("VideoTimeLine", message);
    }
}
