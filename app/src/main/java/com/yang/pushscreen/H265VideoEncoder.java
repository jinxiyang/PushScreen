package com.yang.pushscreen;

import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class H265VideoEncoder implements Runnable{

    //nal类型，取该字节的中间6位，之后右移1位，得到nal类型
    //例如：0x26
    private static final int NAL_I = 19;
    private static final int NAL_B = 0;
    //例如：0x02
    private static final int NAL_P = 1;

    //例如：0x40
    private static final int NAL_VPS = 32;
    private static final int NAL_SPS = 33;
    private static final int NAL_PPS = 34;

    private EncodedDataCallback callBack;

    private MediaCodec mediaCodec;

    //nal分隔符占用的字节数，计算帧类型时需要偏移byte
    //分割符0x00 00 00 01 或者是0x00 00 01
    private int byteOffset;

    //帧类型
    private int nalType;

    //是否在每个I帧之前，添加vps、sps、pps信息。
    // 如果录屏的数据本地保存为mp4，不需要添加；如果网络实时传输，如直播，需要添加
    private boolean addVpsSpsPpsBeforeIFrame = true;

    //保存vps数据，网络传输中放在I帧之前
    private byte[] vps_sps_pps_buf;

    //标志是否编码，false结束编码
    private volatile boolean encoding = true;

    public H265VideoEncoder(@NonNull MediaCodec mediaCodec, @NonNull EncodedDataCallback callBack) {
        this.mediaCodec = mediaCodec;
        this.callBack = callBack;
    }

    @Override
    public void run() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int index;
        while (encoding) {
            index = mediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
            if (index >= 0) {
                ByteBuffer outputBuffer;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = mediaCodec.getOutputBuffer(index);
                } else {
                    outputBuffer = mediaCodec.getOutputBuffers()[index];
                }
                if (outputBuffer == null) {
                    continue;
                }
                onEncodedDataAvailable(outputBuffer, bufferInfo);
                mediaCodec.releaseOutputBuffer(index, false);
            }
        }

        try {
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaCodec = null;
    }

    private void onEncodedDataAvailable(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (addVpsSpsPpsBeforeIFrame){
            addVpsSpsPpsBeforeIFrame(outputBuffer, bufferInfo);
        } else {
            byte[] bytes = new byte[bufferInfo.size];
            outputBuffer.get(bytes);
//            callBack.onEncodedDataAvailable(bytes);
        }
    }

    private void addVpsSpsPpsBeforeIFrame(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo){
        if (outputBuffer.get(2) == 0x01) {
            //分割符0x00 00 01
            byteOffset = 3;
        } else {
            //分割符0x00 00 00 01
            byteOffset = 4;
        }
        //nal类型，取该字节的中间6位，之后右移1位，得到nal类型
        nalType = (outputBuffer.get(byteOffset) & 0x7E) >> 1;

        if (nalType == NAL_VPS) {
            vps_sps_pps_buf = new byte[bufferInfo.size];
            outputBuffer.get(vps_sps_pps_buf);
        } else if (nalType == NAL_I) {
            final byte[] bytes = new byte[vps_sps_pps_buf.length + bufferInfo.size];
            System.arraycopy(vps_sps_pps_buf, 0, bytes, 0, vps_sps_pps_buf.length);
            outputBuffer.get(bytes, vps_sps_pps_buf.length, bufferInfo.size);
//            callBack.onEncodedDataAvailable(bytes);
        } else {
            byte[] bytes = new byte[bufferInfo.size];
            outputBuffer.get(bytes);
//            callBack.onEncodedDataAvailable(bytes);
        }
    }

    public void close(){
        encoding = false;
    }

    //再开始前设置有用
    public void setAddVpsSpsPpsBeforeIFrame(boolean addVpsSpsPpsBeforeIFrame) {
        this.addVpsSpsPpsBeforeIFrame = addVpsSpsPpsBeforeIFrame;
    }
}
