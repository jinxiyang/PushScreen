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
JNIEXPORT jboolean JNICALL
Java_com_yang_pushscreen_PushScreenManager_connectUrl(JNIEnv *env, jobject thiz, jstring url) {
    const char *nUrl = env->GetStringUTFChars(url, 0);
    int ret;
    PushScreen *pushScreen = static_cast<PushScreen *>(malloc(sizeof(PushScreen)));
    memset(pushScreen, 0, sizeof(PushScreen));
    pushScreen->rtmp = RTMP_Alloc();
    RTMP_Init(pushScreen->rtmp);
    pushScreen->rtmp->Link.timeout = 10;
    LOGI("connect %s", nUrl);
    RTMP_SetupURL(pushScreen->rtmp, nUrl);
    env->ReleaseStringUTFChars(url, nUrl);
    return ret;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_yang_pushscreen_PushScreenManager_sendVideoData(JNIEnv *env, jobject thiz, jbyteArray bytes,
                                                         jint len, jlong tms) {


}