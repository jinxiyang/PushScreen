package com.yang.pushscreen;

public interface RtmpPushDataSource {
    void startOutput(RtmpPushQueue queue);
    void stopOutput();
}
