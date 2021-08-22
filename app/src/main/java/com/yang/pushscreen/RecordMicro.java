package com.yang.pushscreen;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.yang.pushscreen.utils.TaskManager;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordMicro implements RtmpPushDataSource{
    private static final String TAG = "PushScreen";

    private AtomicBoolean encoding = new AtomicBoolean(false);

    @Override
    public void startOutput(RtmpPushQueue queue, long startNanoTime){
        TaskManager.getInstance().execute(() -> startRecord(queue, startNanoTime));
    }

    private void startRecord(RtmpPushQueue queue, long startNanoTime) {
        encoding.set(true);
        int sampleRate = 44100;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        Log.i(TAG, "getMinBufferSize: " + minBufferSize);

        MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 2);
        //录音的质量
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        //一秒的码率 aac
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64_000);
        //设置mediacodec的bytebuffer大小
        //默认4096，如果不设置，minBufferSize 大于 4096，会报异常BufferOverflowException
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize);

        MediaCodec mediaCodec = null;
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "startRecord: failed");
            return;
        }
        putAudioHeaderQueue(queue, 0);

        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        audioRecord.startRecording();
        Log.i(TAG, "开始录音");

        long startTimeUs = 0;
        long compensationUs = 0;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        byte[] buffer = new byte[minBufferSize];
        int len, index;
        while (encoding.get()){
            len = audioRecord.read(buffer, 0, buffer.length);
            if (len <= 0){
                continue;
            }
            //立即得到有效的缓冲区
            index = mediaCodec.dequeueInputBuffer(0);
            if (index >= 0){
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                inputBuffer.clear();
                inputBuffer.put(buffer, 0, len);
                //数据进入编码队列，这里时间传微秒，这个不行System.currentTimeMillis()，是毫秒单位
                mediaCodec.queueInputBuffer(index, 0, len, System.nanoTime() / 1000, 0);
            }

            index = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (encoding.get() && index >= 0){
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                byte[] bytes = new byte[bufferInfo.size];
                outputBuffer.get(bytes);
                if (startTimeUs == 0){
                    startTimeUs = bufferInfo.presentationTimeUs;
                    compensationUs = Math.max((System.nanoTime() - startNanoTime) / 1000, 0);
                }
                long tms = (bufferInfo.presentationTimeUs - startTimeUs + compensationUs) / 1000;
                putQueue(queue, bytes, RtmpPacket.RTMP_PACKET_TYPE_AUDIO_DATA, tms);
                mediaCodec.releaseOutputBuffer(index, false);
                index = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        }
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
    }

    private void putAudioHeaderQueue(RtmpPushQueue queue, long pts){
        //发送音频空数据头
        byte[] audioHeader = new byte[]{0x12, 0x08};
        putQueue(queue, audioHeader, RtmpPacket.RTMP_PACKET_TYPE_AUDIO_HEAD, pts);
    }

    private void putQueue(RtmpPushQueue queue, byte[] bytes, int type, long tms){
        Log.i(TAG, "发送音频：类型" + type + "  时间戳：" + tms + "  长度：" + bytes.length);
        RtmpPacket rtmpPacket = new RtmpPacket(type, bytes, bytes.length, tms);
        queue.putPacket(rtmpPacket);
    }

    @Override
    public void stopOutput() {
        encoding.set(false);
    }
}
