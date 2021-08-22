package com.yang.pushscreen;

import android.content.Context;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.util.Log;

import com.yang.pushscreen.utils.SaveDataFile;
import com.yang.pushscreen.utils.TaskManager;

public class RecordScreen implements RtmpPushDataSource{
    private static final String TAG = "PushScreen";

    private Context appContext;
    private MediaProjection mediaProjection;
    private H264VideoEncoder videoEncoder;

    private SaveDataFile saveData;

    public RecordScreen(Context context, MediaProjection mediaProjection) {
        this.appContext = context.getApplicationContext();
        this.mediaProjection = mediaProjection;
    }

    @Override
    public void startOutput(RtmpPushQueue queue, long startTimeMillis) {
        TaskManager.getInstance().execute(() -> startRecord(queue, startTimeMillis));
    }

    private void startRecord(RtmpPushQueue queue, long startTimeMillis){
        saveData = new SaveDataFile(appContext, "record_screen", false);
        try {
            //视频编码器
            MediaCodec mediaCodec = MediaCodecCreator.captureScreen(appContext, mediaProjection, false);
            mediaCodec.start();
            videoEncoder = new H264VideoEncoder(mediaCodec, false, (bytes, tms) -> {
                putPacket(queue, bytes, tms);
                if (saveData != null){
                    saveData.save(bytes);
                }
            });
            videoEncoder.startEncoder(startTimeMillis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "硬件编码器DSP不支持");
        //硬件编码器DSP不支持，使用软编
    }


    private void putPacket(RtmpPushQueue queue, byte[] bytes, long tms){
        Log.i(TAG, "发送视频：" + "  时间戳：" + tms + "  长度：" + bytes.length);
        RtmpPacket packet = new RtmpPacket(RtmpPacket.RTMP_PACKET_TYPE_VIDEO, bytes, bytes.length, tms);
        queue.putPacket(packet);
    }

    @Override
    public void stopOutput() {
        if (videoEncoder != null){
            videoEncoder.close();
        }
    }
}
