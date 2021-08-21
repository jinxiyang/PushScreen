package com.yang.pushscreen;

import android.content.Context;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.util.Log;

import com.yang.pushscreen.utils.SaveDataFile;
import com.yang.pushscreen.utils.TaskManager;

public class PushScreenManager implements Runnable{
    private static final String TAG = "PushScreen";

    private static final String PUSH_LIVE_URL_BILIBILI = "rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_618613652_51916944&key=23482217147672fbf1fff05c3bb06b1a&schedule=rtmp&pflag=1";

    static {
        System.loadLibrary("native-lib");
    }

    private long mNativePtr;

    private Context appContext;
    private MediaProjection mediaProjection;
    private H264VideoEncoder h264VideoEncoder;
    private SaveDataFile saveData;

    private EncodedDataCallback videoCallback = (bytes, tms) -> {
        if (saveData != null){
            saveData.save(bytes);
        }
        sendVideoData(bytes, bytes.length, tms);
    };

    public PushScreenManager(Context context, MediaProjection mediaProjection) {
        this.appContext = context.getApplicationContext();
        this.mediaProjection = mediaProjection;
    }

    public void start(){
        TaskManager.getInstance().execute(this);
    }

    @Override
    public void run() {
        if (!connectUrl(PUSH_LIVE_URL_BILIBILI)){
            Log.i(TAG, "connectUrl: failed");
            return;
        }

        saveData = new SaveDataFile(appContext, "capture_screen", false);
        try {
            MediaCodec mediaCodec = MediaCodecCreator.captureScreen(appContext, mediaProjection, false);
            //启动编码器
            mediaCodec.start();
            h264VideoEncoder = new H264VideoEncoder(mediaCodec, videoCallback);
            h264VideoEncoder.setAddSpsPpsBeforeIFrame(false);
            TaskManager.getInstance().execute(h264VideoEncoder);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "硬件编码器DSP不支持");
        //硬件编码器DSP不支持，使用软编
        //TODO
    }

    public void stop() {
        disconnectRtmp();
        if (mediaProjection != null){
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (h264VideoEncoder != null){
            h264VideoEncoder.close();
            h264VideoEncoder = null;
        }
    }

    private boolean connectUrl(String url){
        if (mNativePtr == 0){
            mNativePtr = nativeConnectUrl(url);
        }
        return mNativePtr != 0;
    }

    private void disconnectRtmp(){
        nativeDisconnect(mNativePtr);
    }

    private void sendVideoData(byte[] bytes, int len, long tms){
        Log.i(TAG, "sendVideoData: 长度 - " + len + "   时间戳 - " + tms);
        nativeSendVideoData(mNativePtr, bytes, len, tms);
    }

    private native long nativeConnectUrl(String url);

    private native void nativeDisconnect(long ptr);

    private native boolean nativeSendVideoData(long ptr, byte[] bytes, int len, long tms);
}
