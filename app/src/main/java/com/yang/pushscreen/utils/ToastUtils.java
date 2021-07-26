package com.yang.pushscreen.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ToastUtils {

    private static Toast mToast;

    private static Handler mHandler;

    /**
     * 显示短时间的Toast
     * @param text
     */
    public static void showShort(Context context, CharSequence text){
        show(context, text, Toast.LENGTH_SHORT);
    }

    /**
     * 显示长时间的Toast
     * @param text
     */
    public static void showLong(Context context, CharSequence text){
        show(context, text, Toast.LENGTH_LONG);
    }

    private static void show(final Context context, final CharSequence text, final int duration){
        if (mToast != null){
            mToast.cancel();
        }
        if (context == null){
            return;
        }

        final Context applicationContext = context.getApplicationContext();
        if (Looper.getMainLooper() == Looper.myLooper()){
            mToast = Toast.makeText(applicationContext, text, duration);
            mToast.show();
        }else {
            if (mHandler == null){
                mHandler = new Handler(Looper.getMainLooper());
            }
            mHandler.post(() -> {
                mToast = Toast.makeText(applicationContext, text, duration);
                mToast.show();
            });
        }
    }
}
