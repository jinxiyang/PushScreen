package com.yang.pushscreen;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

public class MediaCodecCreator {

    /**
     * 得到一个录屏MediaCodec
     * @param context
     * @param mediaProjection
     * @return
     * @throws IOException
     */
    public static MediaCodec captureScreen(Context context, @NonNull MediaProjection mediaProjection, boolean h265) throws IOException {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(dm);
        //屏幕的尺寸信息
//        int screenHeight = dm.heightPixels;
//        int screenWidth = dm.widthPixels;
//        int screenDpi = dm.densityDpi;
        int screenHeight = 720;
        int screenWidth = 1080;
        int screenDpi = 1;

        //编码格式
        String mimeType;
        if (h265){
            mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC;
        } else {
            //h264
            mimeType = MediaFormat.MIMETYPE_VIDEO_AVC;
        }
        MediaCodec mediaCodec = MediaCodec.createEncoderByType(mimeType);
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, screenWidth, screenHeight);
        //设置编码的颜色格式，这里是通过Surface
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //1080P最好在5Mbps/5120Kbps到8Mbps/8192Kbps之间,因为低于5Mbps不够清晰,而大于8Mbps视频文件会过大，比如我们设置8Mbps,则是1024*1024*8
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1024 * 1024 * 8);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1024 * 1024 * 5);
        //帧率
//        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        //关键帧间隔时间，单位为秒，此处的意思是这个视频每两秒一个关键帧
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 4);
//        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);


        //CQ 对应于 OMX_Video_ControlRateDisable，它表示完全不控制码率，尽最大可能保证图像质量；
        //CBR 对应于 OMX_Video_ControlRateConstant，它表示编码器会尽量把输出码率控制为设定值，即我们前面提到的“不为所动”；
        //VBR 对应于 OMX_Video_ControlRateVariable，它表示编码器会根据图像内容的复杂度（实际上是帧间变化量的大小）来动态调整输出码率，图像复杂则码率高，图像简单则码率低；
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        //最后一个参数需要注意，标明配置的是编码器
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        //通过Surface的方式喂数据，所以需要通过encoder创建输入Surface
        Surface surface = mediaCodec.createInputSurface();

        //录屏到虚拟Surface上
        mediaProjection.createVirtualDisplay("projection-screen",
                screenWidth,
                screenHeight,
                screenDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface,
                null,
                null);
        return mediaCodec;
    }


}
