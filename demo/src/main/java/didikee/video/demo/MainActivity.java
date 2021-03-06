package didikee.video.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.github.video.view.VideoRangeSeekBar;

import didikee.github.helper.utils.DisplayUtil;
import didikee.github.helper.utils.UriUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private LinearLayout rootLayout;
    private VideoRangeSeekBar videoRangeSeekBar;
    private static final int REQUEST_FOR_VIDEO = 3333;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.bt_action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO 选择照片
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/mp4");//设置类型，我这里是任意类型，任意后缀的可以这样写
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_FOR_VIDEO);
            }
        });

        rootLayout = findViewById(R.id.root_layout);
    }

    private void initVideoView(String videoPath) {
        if (TextUtils.isEmpty(videoPath)) {
            return;
        }
        if (videoRangeSeekBar != null) {
            return;
        }

        videoRangeSeekBar = new VideoRangeSeekBar(this);
        videoRangeSeekBar.setOnVideoRangeSeekBarListener(new VideoRangeSeekBar.VideoRangeSeekBarListener() {
            @Override
            public void onLeftProgressChanged(float progress) {
                Log.d(TAG, "onLeftProgressChanged: " + progress);
            }

            @Override
            public void onRightProgressChanged(float progress) {
                Log.d(TAG, "onRightProgressChanged: " + progress);
            }

            @Override
            public void onPlayProgressChanged(float progress) {
                Log.d(TAG, "onPlayProgressChanged: " + progress);
            }

            @Override
            public void didStartDragging() {
                Log.d(TAG, "didStartDragging");
            }

            @Override
            public void didStopDragging() {
                Log.d(TAG, "didStopDragging");
            }
        });
        rootLayout.addView(videoRangeSeekBar, ViewGroup.LayoutParams.MATCH_PARENT, DisplayUtil.dp2px(this, 64));
        videoRangeSeekBar.setVideoPath(videoPath);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            String mediaPath = UriUtil.getPathFromUri(this, data.getData());
            if (!TextUtils.isEmpty(mediaPath)) {
                boolean isVideo = mediaPath.toLowerCase().endsWith(".mp4");
                if (isVideo) {
                    //video
                    initVideoView(mediaPath);
                }
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
