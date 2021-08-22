package com.yang.pushscreen;

import android.util.Log;

import com.yang.pushscreen.utils.TaskManager;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class RtmpPushManager {
    private static final String TAG = "RtmpPush";

    private RtmpPushDataSource mVideoSource;
    private RtmpPushDataSource mAudioSource;

    private String mRtmpUrl;
    private AtomicBoolean mPushing;
    private LinkedBlockingQueue<RtmpPacket> mRtmpPacketQueue = new LinkedBlockingQueue<>();

    private OnConnectRtmpUrlListener mOnConnectRtmpUrlListener;

    public RtmpPushManager(RtmpPushDataSource videoSource, RtmpPushDataSource audioSource, String rtmpUrl) {
        this.mVideoSource = videoSource;
        this.mAudioSource = audioSource;
        this.mRtmpUrl = rtmpUrl;
    }

    public void start(){
        //开启子线程
        TaskManager.getInstance().execute(this::startRtmpPush);
    }

    private void startRtmpPush() {
        //连接是耗时操作
        RtmpPush rtmpPush = RtmpPush.connect(mRtmpUrl);
        if (rtmpPush == null){
            Log.i(TAG, "connectUrl: failed");
            onConnectedRtmp(false);
            return;
        }
        onConnectedRtmp(true);
        mPushing = new AtomicBoolean(true);
        if (mVideoSource != null){
            mVideoSource.startOutput(this::putPacket);
        }
        if (mAudioSource != null){
            mAudioSource.startOutput(this::putPacket);
        }
        while (mPushing.get()){
            RtmpPacket rtmpPacket = null;
            try {
                rtmpPacket = mRtmpPacketQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "mRtmpPacketQueue take: 取出数据");
            if (rtmpPacket != null){
                rtmpPush.sendData(rtmpPacket);
            }
        }
        if (mVideoSource != null){
            mVideoSource.stopOutput();
        }
        if (mAudioSource != null){
            mAudioSource.stopOutput();
        }
        rtmpPush.disconnect();
    }

    public void stop() {
        mPushing.set(false);
    }

    private void onConnectedRtmp(boolean success){
        if (mOnConnectRtmpUrlListener != null){
            mOnConnectRtmpUrlListener.onConnectRtmpUrl(success);
        }
    }

    public void setOnConnectRtmpUrlListener(OnConnectRtmpUrlListener onConnectRtmpUrlListener) {
        this.mOnConnectRtmpUrlListener = onConnectRtmpUrlListener;
    }

    public void putPacket(RtmpPacket rtmpPacket) {
        if (!mPushing.get()){
            return;
        }
        try {
            mRtmpPacketQueue.add(rtmpPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
