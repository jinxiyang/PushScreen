package com.yang.pushscreen;

public class RtmpPush {

    static {
        System.loadLibrary("rtmppush");
    }

    private long mNativePtr;
    private String mUrl;

    private RtmpPush(String url, long ptr) {
        this.mUrl = url;
        this.mNativePtr = ptr;
    }

    public static RtmpPush connect(String url){
        long ptr = nativeConnectUrl(url);
        if (ptr != 0){
            return new RtmpPush(url, ptr);
        } else {
            return null;
        }
    }

    public void disconnect(){
        nativeDisconnect(mNativePtr);
    }

    public void sendData(RtmpPacket rtmpPacket){
        nativeSendData(mNativePtr, rtmpPacket.getType(), rtmpPacket.getData(), rtmpPacket.getLen(), rtmpPacket.getTms());
    }

    private static native long nativeConnectUrl(String url);

    private static native void nativeDisconnect(long ptr);

    private static native boolean nativeSendData(long ptr, int type, byte[] bytes, int len, long tms);
}
