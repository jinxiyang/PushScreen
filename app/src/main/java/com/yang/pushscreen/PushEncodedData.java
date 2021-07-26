package com.yang.pushscreen;

import com.yang.pushscreen.utils.SaveDataFile;

public class PushEncodedData implements EncodedDataCallback {

    private SaveDataFile saveData;

    @Override
    public void onEncodedDataAvailable(byte[] bytes, long tms) {
        if (saveData != null){
            saveData.save(bytes);
        }
    }

    public void setSaveData(SaveDataFile saveData) {
        this.saveData = saveData;
    }

}
