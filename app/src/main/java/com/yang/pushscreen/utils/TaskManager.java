package com.yang.pushscreen.utils;

import androidx.annotation.NonNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskManager {

    private static volatile TaskManager instance;
    private ThreadPoolExecutor threadPoolExecutor;

    private TaskManager(){
        int cpuCount = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(2, Math.min(cpuCount - 1, 4));
        int maxPoolSize = Math.max(corePoolSize, cpuCount * 2 + 1);
        threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(5));
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    public static TaskManager getInstance(){
        if (instance == null){
            synchronized (TaskManager.class){
                if (instance == null){
                    instance = new TaskManager();
                }
            }
        }
        return instance;
    }

    public void execute(@NonNull Runnable runnable){
        threadPoolExecutor.execute(runnable);
    }
}
