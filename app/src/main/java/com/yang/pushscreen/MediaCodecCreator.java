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
    public static @Nullable MediaCodec captureScreen(Context context, @NonNull MediaProjection mediaProjection, boolean h265) throws IOException {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(dm);
        //屏幕的尺寸信息
        int screenHeight = dm.heightPixels;
        int screenWidth = dm.widthPixels;
        int screenDpi = dm.densityDpi;

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
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1024 * 1024 * 8);
        //帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        //关键帧间隔时间，单位为秒，此处的意思是这个视频每两秒一个关键帧
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
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
