#include <jni.h>
#include <string>
#include <android/log.h>
#include <malloc.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "PushNative", __VA_ARGS__)

extern "C" {
#include "librtmp/rtmp.h"
}

typedef struct {
    int16_t sps_len;
    int16_t pps_len;
    int8_t *sps;
    int8_t *pps;
    RTMP *rtmp;
} PushScreen;

RTMPPacket *createVideoPacket(PushScreen *screen, int8_t *data, int len, long tms);

extern "C"
JNIEXPORT jlong JNICALL
Java_com_yang_pushscreen_PushScreenManager_nativeConnectUrl(JNIEnv *env, jobject thiz, jstring url) {
    const char *nUrl = env->GetStringUTFChars(url, 0);
    LOGI("nativeConnectUrl %s", nUrl);
    PushScreen *pushScreen = static_cast<PushScreen *>(malloc(sizeof(PushScreen)));
    int ret;
    do {
        //内存初始化
        memset(pushScreen, 0, sizeof(PushScreen));
        //申请内存
        pushScreen->rtmp = RTMP_Alloc();
        //初始化
        RTMP_Init(pushScreen->rtmp);
        //设置链接超时时间，秒
        pushScreen->rtmp->Link.timeout = 10;

        LOGI("RTMP_SetupURL %s", nUrl);
        //设置地址
        if (!(ret = RTMP_SetupURL(pushScreen->rtmp, (char *) nUrl))) break;
        //开启输出模式
        RTMP_EnableWrite(pushScreen->rtmp);

        LOGI("RTMP_Connect");
        //连接服务器
        if (!(ret = RTMP_Connect(pushScreen->rtmp, 0))) break;

        LOGI("RTMP_ConnectStream");
        //连接流
        if (!(ret = RTMP_ConnectStream(pushScreen->rtmp, 0))) break;

        LOGI("RTMP_Connect Success");
    } while (0);

    if (!ret && pushScreen){
        free(pushScreen);
        pushScreen = nullptr;
    }
    env->ReleaseStringUTFChars(url, nUrl);
    return reinterpret_cast<jlong>(pushScreen);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_yang_pushscreen_PushScreenManager_nativeDisconnect(JNIEnv *env, jobject thiz, jlong ptr) {
    PushScreen *pushScreen = reinterpret_cast<PushScreen *>(ptr);
    if (pushScreen){
        if (pushScreen->rtmp && RTMP_IsConnected(pushScreen->rtmp)){
            RTMP_Close(pushScreen->rtmp);
            RTMP_Free(pushScreen->rtmp);
        }
        free(pushScreen);
        pushScreen = nullptr;
    }
}

void cacheSpsPps(PushScreen *pushScreen, int8_t *data, int len) {
    LOGI("cacheSpsPps start");
    //sps+pps
    //00 00 00 01 67 42C032DA8110049D9679A80808083C2010A8 00 00 00 01 68 CE 3C 80
//    for (int i = len - 1; i >= 0; i--) {
//        if (i - 4 >= 0
//            && data[i] == 0x68
//            && data[i - 1] == 0x01
//            && data[i - 2] == 0x00
//            && data[i - 3] == 0x00
//            && data[i - 4] == 0x00) {
//
//            //sps
//            pushScreen->sps_len = i - 4 - 4;
//            pushScreen->sps = static_cast<int8_t *>(malloc(pushScreen->sps_len));
//            memcpy(pushScreen->sps, data + 4, pushScreen->sps_len);
//
//            //pps
//            pushScreen->pps_len = len - i;
//            pushScreen->pps = static_cast<int8_t *>(malloc(pushScreen->pps_len));
//            memcpy(pushScreen->pps, data + i, pushScreen->pps_len);
//            break;
//        }
//    }

    for (int i = 0; i < len; i++) {
        //0x00 0x00 0x00 0x01
        if (i + 4 < len) {
            if (data[i] == 0x00 && data[i + 1] == 0x00
                && data[i + 2] == 0x00
                && data[i + 3] == 0x01) {
                //0x00 0x00 0x00 0x01 7 sps 0x00 0x00 0x00 0x01 8 pps
                //将sps pps分开
                //找到pps
                if (data[i + 4]  == 0x68) {
                    //去掉界定符
                    pushScreen->sps_len = i - 4;
                    pushScreen->sps = static_cast<int8_t *>(malloc(pushScreen->sps_len));
                    memcpy(pushScreen->sps, data + 4, pushScreen->sps_len);

                    pushScreen->pps_len = len - (4 + pushScreen->sps_len) - 4;
                    pushScreen->pps = static_cast<int8_t *>(malloc(pushScreen->pps_len));
                    memcpy(pushScreen->pps, data + 4 + pushScreen->sps_len + 4, pushScreen->pps_len);
                    LOGI("sps:%d pps:%d", pushScreen->sps_len, pushScreen->pps_len);
                    break;
                }
            }
        }
    }
    LOGI("cacheSpsPps end");
}

int sendRTMPPacket(PushScreen *pushScreen, RTMPPacket *packet){
    int ret = RTMP_SendPacket(pushScreen->rtmp, packet, 1);
    LOGI("RTMP_SendPacket success");
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

RTMPPacket *createSpsPpsPacket(PushScreen *pushScreen){
    LOGI("createSpsPpsPacket");

    //sps pps Packet
    int body_size = 16 + pushScreen->sps_len + pushScreen->pps_len;

    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    //初始化
    RTMPPacket_Alloc(packet, body_size);
    int i = 0;
    packet->m_body[i++] = 0x17;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    //版本
    packet->m_body[i++] = 0x01;
    //编码规格  sps[1] + sps[2] + sps[3]
    packet->m_body[i++] = pushScreen->sps[1];
    packet->m_body[i++] = pushScreen->sps[2];
    packet->m_body[i++] = pushScreen->sps[3];
    //表示NALU的长度，几个字节。4个字节，(0xFF & 3) + 1 = 4
    packet->m_body[i++] = 0xFF;

    //sps个数，1个。  0xE1 & 0x1F = 1
    packet->m_body[i++] = 0xE1;
    //sps长度
    //高8位
    packet->m_body[i++] = (pushScreen->sps_len >> 8) & 0xFF;
    //低8位
    packet->m_body[i++] = pushScreen->sps_len & 0xFF;
    //拷贝sps
    memcpy(&packet->m_body[i], pushScreen->sps, pushScreen->sps_len);
    i += pushScreen->sps_len;

    //pps个数，1个
    packet->m_body[i++] = 0x01;
    //pps长度
    //高8位
    packet->m_body[i++] = (pushScreen->pps_len >> 8) & 0xFF;
    //低8位
    packet->m_body[i++] = pushScreen->pps_len & 0xFF;
    //拷贝pps
    memcpy(&packet->m_body[i], pushScreen->pps, pushScreen->pps_len);
    
    //packet类型 视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = pushScreen->rtmp->m_stream_id;
    return packet;
}

RTMPPacket *createVideoPacket(PushScreen *pushScreen, int8_t *data, int len, long tms) {
    LOGI("createVideoPacket");
    //丢弃分隔符
    data += 4;
    len -= 4;

    //video Packet
    int body_size = 9 + len;

    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    //初始化
    RTMPPacket_Alloc(packet, body_size);

    int i = 0;
    if (data[0] == 0x65){
        packet->m_body[i++] = 0x17;
    } else {
        packet->m_body[i++] = 0x27;
    }
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //4个字节表示数据长度
    packet->m_body[i++] = (len >> 24) & 0xFF;
    packet->m_body[i++] = (len >> 16) & 0xFF;
    packet->m_body[i++] = (len >> 8) & 0xFF;
    packet->m_body[i++] = len & 0xFF;

    //拷贝数据
    memcpy(&packet->m_body[i], data, len);

    //packet类型 视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = pushScreen->rtmp->m_stream_id;
    return packet;
}

int sendVideoData(PushScreen *pushScreen, int8_t *data, int len, long tms) {
    LOGI("sendVideoData");

    //缓存sps+pps到PushScreen，不需要推流
    if (data[4] == 0x67){
        //sps+pps
        //00 00 00 01 67 42C032DA8110049D9679A80808083C2010A8 00 00 00 01 68 CE 3C 80
        if (!pushScreen->sps || !pushScreen->pps){
            cacheSpsPps(pushScreen, data, len);
        }
        return len;
    }

    //I帧
    //00 00 00 01 65 B84.....
    if (data[4] == 0x65){
        //推送sps+pps
        RTMPPacket *packet = createSpsPpsPacket(pushScreen);
        sendRTMPPacket(pushScreen, packet);
    }

    RTMPPacket *packet = createVideoPacket(pushScreen, data, len, tms);
    return sendRTMPPacket(pushScreen, packet);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_yang_pushscreen_PushScreenManager_nativeSendVideoData(JNIEnv *env, jobject thiz, jlong ptr,
                                                               jbyteArray bytes, jint len,
                                                               jlong tms) {
    PushScreen *pushScreen = reinterpret_cast<PushScreen *>(ptr);
    if (!pushScreen){
        LOGI("nativeSendVideoData ptr is NULL");
        return 0;
    }
    LOGI("nativeSendVideoData");
    int ret;
    jbyte *videoData = env->GetByteArrayElements(bytes, NULL);
    ret = sendVideoData(pushScreen, videoData, len, tms);
    env->ReleaseByteArrayElements(bytes, videoData, 0);
    return ret;
}

