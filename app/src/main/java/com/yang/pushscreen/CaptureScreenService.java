package com.yang.pushscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static android.app.Activity.RESULT_OK;


public class CaptureScreenService extends Service {
    private static final String TAG = "PushScreen";

    public static final int EVENT_INVALID = 0;
    public static final int EVENT_START_CAPTURE_SCREEN = 1;
    public static final int EVENT_STOP_CAPTURE_SCREEN = 2;
    public static final String KEY_INTENT_DATA = "intent_data";
    public static final String KEY_EVENT = "event";

    private static final int NOTIFICATION_ID = 1;


    private PushScreenManager pushScreenManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notification foregroundNotification = createForegroundNotification();
        startForeground(NOTIFICATION_ID, foregroundNotification);
    }

    private Notification createForegroundNotification(){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // 唯一的通知通道的id.
        String notificationChannelId = "notification_channel_id_01";
        // Android8.0以上的系统，新建消息通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //用户可见的通道名称
            String channelName = "前台服务通知";
            //通道的重要程度
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(notificationChannelId, channelName, importance);
            notificationChannel.setDescription("常驻前台服务，用于录屏");
            //LED灯
            notificationChannel.enableLights(true);
            //震动
            //notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            //notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationChannelId);
        //通知小图标
        builder.setSmallIcon(R.mipmap.ic_launcher);
        //通知标题
        builder.setContentTitle("录屏服务");
        //通知内容
        builder.setContentText("请勿关闭，保持开启");
        //设定通知显示的时间
        builder.setWhen(System.currentTimeMillis());
        //设定启动的内容
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        //创建通知并返回
        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int event = intent.getIntExtra(KEY_EVENT, EVENT_INVALID);
        if (event == EVENT_START_CAPTURE_SCREEN){
            Intent intentData = intent.getParcelableExtra(KEY_INTENT_DATA);
            startCaptureScreen(intentData);
            Log.i(TAG, "开启录屏");
        } else if (event == EVENT_STOP_CAPTURE_SCREEN){
            stopCaptureScreen();
            stopForeground(true);
            Log.i(TAG, "停止录屏");
        }
        return super.onStartCommand(intent, flags, startId);
    }


    private void startCaptureScreen(Intent data) {
        if (pushScreenManager != null){
            Log.i(TAG, "已经在录屏");
            return;
        }
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(RESULT_OK, data);
        pushScreenManager = new PushScreenManager(this, mediaProjection);
        pushScreenManager.start();
    }

    private void stopCaptureScreen(){
        if (pushScreenManager != null){
            pushScreenManager.stop();
            pushScreenManager = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCaptureScreen();
    }
}
