package com.yang.pushscreen;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.yang.pushscreen.utils.ToastUtils;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_MEDIA_PROJECTION = 0x123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> attemptStartCaptureScreen());
        btnStop.setOnClickListener(v -> stopCaptureScreen());
    }

    private void attemptStartCaptureScreen(){
        //录屏MediaProjectionManager，需要Android 5.0及以上版本
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
        //申请录屏权限
        startActivityForResult(screenCaptureIntent, REQUEST_CODE_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION){
            if (resultCode == RESULT_OK && data != null){
                startCaptureScreen(data);
            } else {
                ToastUtils.showShort(this, "没有录屏权限");
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startCaptureScreen(Intent data) {
        //录屏在安卓高版本10，需要开启前台service、前台通知
        //Manifest文件需要android:foregroundServiceType="mediaProjection"
        Intent intent = new Intent(this, CaptureScreenService.class);
        intent.putExtra(CaptureScreenService.KEY_EVENT, CaptureScreenService.EVENT_START_CAPTURE_SCREEN);
        intent.putExtra(CaptureScreenService.KEY_INTENT_DATA, data);
        startService(intent);
    }

    private void stopCaptureScreen(){
        Intent intent = new Intent(this, CaptureScreenService.class);
        intent.putExtra(CaptureScreenService.KEY_EVENT, CaptureScreenService.EVENT_STOP_CAPTURE_SCREEN);
        startService(intent);
    }
}