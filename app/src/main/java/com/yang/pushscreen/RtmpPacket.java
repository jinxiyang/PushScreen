package com.yang.pushscreen;

public class RtmpPacket {
    public static final int RTMP_PACKET_TYPE_VIDEO = 0;
    public static final int RTMP_PACKET_TYPE_AUDIO_HEAD = 1;
    public static final int RTMP_PACKET_TYPE_AUDIO_DATA = 2;

    private int type;
    private byte[] data;
    private int len;
    private long tms;

    public RtmpPacket() {
    }

    public RtmpPacket(int type, byte[] data, int len, long tms) {
        this.type = type;
        this.data = data;
        this.len = len;
        this.tms = tms;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public long getTms() {
        return tms;
    }

    public void setTms(long tms) {
        this.tms = tms;
    }
}
