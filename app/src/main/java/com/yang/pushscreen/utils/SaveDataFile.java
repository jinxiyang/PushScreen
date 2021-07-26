package com.yang.pushscreen.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveDataFile {

    private FileWriter fileWriter;
    private FileOutputStream fileOutputStream;

    private final char[] HEX_CHAR_TABLE = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public SaveDataFile(Context context, String fileName, boolean h265) {
        File cacheDir = context.getCacheDir();
        String videoFormat;
        if (h265){
            videoFormat = "h265";
        } else {
            videoFormat = "h264";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd_HH:mm:ss");
        String time = sdf.format(new Date());
        String textFileName = String.format("%s_%s_%s.txt", fileName, videoFormat, time);
        String videoFileName = String.format("%s_%s.%s", fileName, time, videoFormat);
        try {
            File textFile = new File(cacheDir.getAbsolutePath() + File.separator + textFileName);
            File videoFile = new File(cacheDir.getAbsolutePath() + File.separator + videoFileName);
            fileWriter = new FileWriter(textFile, true);
            fileOutputStream = new FileOutputStream(videoFile, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(byte[] bytes){
        writeRawData(bytes);
        writeString(bytes);
    }

    private void writeRawData(byte[] bytes){
        if (fileOutputStream != null){
            try {
                fileOutputStream.write(bytes);
                fileOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeString(byte[] bytes) {
        if (fileWriter != null){
            StringBuilder sb = new StringBuilder();

            for (byte b : bytes) {
                sb.append(HEX_CHAR_TABLE[(b & 0xF0) >> 4]);
                sb.append(HEX_CHAR_TABLE[b & 0x0F]);
            }

            sb.append("\n");

            try {
                fileWriter.write(sb.toString());
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeFile() {
        if (fileWriter != null){
            try {
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileWriter = null;
        }

        if (fileOutputStream != null){
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileOutputStream = null;
        }
    }
}
