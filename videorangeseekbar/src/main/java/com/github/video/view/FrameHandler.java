package com.github.video.view;

import android.graphics.Bitmap;

/**
 * user author: didikee
 * create time: 2020-01-02 16:53
 * description: 
 */
public interface FrameHandler {

    /**
     * 是否可用
     * @return
     */
    boolean isAvailable();

    long getDuration();

    Bitmap getFrameAtTime(long position);

    void onDestroy();
}
