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

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_yang_pushscreen_PushScreenManager_nativeSendVideoData(JNIEnv *env, jobject thiz, jbyteArray bytes, jint len, jlong tms) {


}

