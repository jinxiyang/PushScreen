package com.yang.pushscreen;

public interface RtmpPushDataSource {
    void startOutput(RtmpPushQueue queue, long startNanoTime);
    void stopOutput();
}
