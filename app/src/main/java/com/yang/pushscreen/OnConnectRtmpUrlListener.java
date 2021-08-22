package com.yang.pushscreen;

public interface OnConnectRtmpUrlListener {
    /**
     * 连接rtmp成功/失败的回调，回调在子线程
     * @param success
     */
    void onConnectRtmpUrl(boolean success);
}
