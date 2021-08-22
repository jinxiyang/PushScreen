package com.yang.pushscreen;

public interface RtmpPushQueue {
    void putPacket(RtmpPacket rtmpPacket);
}
