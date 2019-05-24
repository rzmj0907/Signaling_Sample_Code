package com.netease.signalling.demo.app;

import android.app.Application;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.signalling.demo.model.CacheInfo;
import com.netease.signalling.demo.model.CurrentUserSettings;
import com.netease.signalling.demo.model.WorkerThread;

public class DemoApplication extends Application {

    private WorkerThread mWorkerThread;

    @Override
    public void onCreate() {
        super.onCreate();
        //SDK初始化
        NIMClient.init(this, null, null);
        CacheInfo.init(this);
    }


    public synchronized void initWorkerThread() {
        if (mWorkerThread == null) {
            mWorkerThread = new WorkerThread(getApplicationContext());
            mWorkerThread.start();
            mWorkerThread.waitForReady();
        }
    }

    public synchronized WorkerThread getWorkerThread() {
        return mWorkerThread;
    }

    public static final CurrentUserSettings mVideoSettings = new CurrentUserSettings();

}
