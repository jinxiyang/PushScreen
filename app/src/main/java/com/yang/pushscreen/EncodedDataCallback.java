package com.yang.pushscreen;

/**
 * 编码后的数据
 */
public interface EncodedDataCallback {
    /**
     * 编码后的数据可用时。子线程
     * @param bytes
     * @param tms
     */
    void onEncodedDataAvailable(byte[] bytes, long tms);
}
