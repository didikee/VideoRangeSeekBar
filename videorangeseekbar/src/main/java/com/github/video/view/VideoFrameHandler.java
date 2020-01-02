package com.github.video.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

/**
 * user author: didikee
 * create time: 2020-01-02 16:54
 * description: 
 */
public class VideoFrameHandler implements FrameHandler {
    private static final Object sync = new Object();
    private long videoLength;
    private MediaMetadataRetriever mediaMetadataRetriever;

    public VideoFrameHandler(String path) {
        mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(path);
            String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            videoLength = Long.parseLong(duration);
        } catch (Exception e) {
        }
    }

    public VideoFrameHandler(Context context, Uri videoUri) {
        mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(context, videoUri);
            String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            videoLength = Long.parseLong(duration);
        } catch (Exception e) {
        }
    }

    @Override
    public boolean isAvailable() {
        return mediaMetadataRetriever != null && videoLength > 0;
    }

    @Override
    public long getDuration() {
        return videoLength;
    }

    @Override
    public Bitmap getFrameAtTime(long position) {
        return mediaMetadataRetriever.getFrameAtTime(position * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
    }

    @Override
    public void onDestroy() {
        synchronized (sync) {
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                    mediaMetadataRetriever = null;
                }
            } catch (Exception e) {
            }
        }
    }

}
