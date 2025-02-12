package com.netease.signalling.demo.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;

import com.netease.signalling.demo.R;

import java.io.File;
import java.io.IOException;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

import static io.agora.rtc.Constants.LOG_FILTER_DEBUG;

public class WorkerThread extends Thread {

    private final static String TAG = WorkerThread.class.getSimpleName();

    private final Context mContext;

    private static final int ACTION_WORKER_THREAD_QUIT = 0X1010; // quit this thread

    private static final int ACTION_WORKER_JOIN_CHANNEL = 0X2010;

    private static final int ACTION_WORKER_LEAVE_CHANNEL = 0X2011;

    private static final int ACTION_WORKER_CONFIG_ENGINE = 0X2012;

    private static final int ACTION_WORKER_PREVIEW = 0X2014;

    private static final class WorkerThreadHandler extends Handler {

        private WorkerThread mWorkerThread;

        WorkerThreadHandler(WorkerThread thread) {
            this.mWorkerThread = thread;
        }

        public void release() {
            mWorkerThread = null;
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mWorkerThread == null) {
                Log.w(TAG, "handler is already released! " + msg.what);
                return;
            }
            switch (msg.what) {
                case ACTION_WORKER_THREAD_QUIT:
                    mWorkerThread.exit();
                    break;
                case ACTION_WORKER_JOIN_CHANNEL:
                    String[] data = (String[]) msg.obj;
                    mWorkerThread.joinChannel(data[0], msg.arg1);
                    break;
                case ACTION_WORKER_LEAVE_CHANNEL:
                    String channel = (String) msg.obj;
                    mWorkerThread.leaveChannel(channel);
                    break;
                case ACTION_WORKER_CONFIG_ENGINE:
                    Object[] configData = (Object[]) msg.obj;
                    mWorkerThread.configEngine((VideoEncoderConfiguration.VideoDimensions) configData[0],
                                               (String) configData[1], (String) configData[2]);
                    break;
                case ACTION_WORKER_PREVIEW:
                    Object[] previewData = (Object[]) msg.obj;
                    mWorkerThread.preview((boolean) previewData[0], (SurfaceView) previewData[1], (int) previewData[2]);
                    break;
            }
        }
    }

    private WorkerThreadHandler mWorkerHandler;

    private boolean mReady;

    public final void waitForReady() {
        while (!mReady) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "wait for " + WorkerThread.class.getSimpleName());
        }
    }

    @Override
    public void run() {
        Log.i(TAG, "start to run");
        Looper.prepare();
        mWorkerHandler = new WorkerThreadHandler(this);
        ensureRtcEngineReadyLock();
        mReady = true;
        // enter thread looper
        Looper.loop();
    }

    private RtcEngine mRtcEngine;

    public final void enablePreProcessor() {
    }

    public final void setPreParameters(float lightness, int smoothness) {
        Constant.PRP_DEFAULT_LIGHTNESS = lightness;
        Constant.PRP_DEFAULT_SMOOTHNESS = smoothness;
    }

    public final void disablePreProcessor() {
    }

    public final void joinChannel(final String channel, int uid) {
        if (Thread.currentThread() != this) {
            Log.w(TAG, "joinChannel() - worker thread asynchronously " + channel + " " + uid);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_JOIN_CHANNEL;
            envelop.obj = new String[]{channel};
            envelop.arg1 = uid;
            mWorkerHandler.sendMessage(envelop);
            return;
        }
        ensureRtcEngineReadyLock();
        mRtcEngine.enableLastmileTest();
        mRtcEngine.addHandler(new IRtcEngineEventHandler() {

            @Override
            public void onLastmileQuality(int quality) {
                super.onLastmileQuality(quality);
                Log.d(TAG, "onLastmileQuality() called with: quality = [" + quality + "]");
                mRtcEngine.disableLastmileTest();
            }

            @Override
            public void onLastmileProbeResult(LastmileProbeResult result) {
                super.onLastmileProbeResult(result);
                Log.d(TAG, "onLastmileProbeResult() called with: result = [" + result + "]");
            }
        });
        mRtcEngine.joinChannel(null, channel, "OpenVCall", uid);
        mEngineConfig.mChannel = channel;
        enablePreProcessor();
        Log.d(TAG, "joinChannel " + channel + " " + uid);
    }

    public final void leaveChannel(String channel) {
        if (Thread.currentThread() != this) {
            Log.w(TAG, "leaveChannel() - worker thread asynchronously " + channel);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_LEAVE_CHANNEL;
            envelop.obj = channel;
            mWorkerHandler.sendMessage(envelop);
            return;
        }
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            mRtcEngine.enableVideo();
        }
        disablePreProcessor();
        mEngineConfig.reset();
        Log.d(TAG, "leaveChannel " + channel);
    }

    private EngineConfig mEngineConfig;

    public final EngineConfig getEngineConfig() {
        return mEngineConfig;
    }

    private final MyEngineEventHandler mEngineEventHandler;

    public final void configEngine(VideoEncoderConfiguration.VideoDimensions videoDimension, String encryptionKey,
                                   String encryptionMode) {
        if (Thread.currentThread() != this) {
            Log.w(TAG, "configEngine() - worker thread asynchronously " + videoDimension + " " + encryptionMode);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_CONFIG_ENGINE;
            envelop.obj = new Object[]{videoDimension, encryptionKey, encryptionMode};
            mWorkerHandler.sendMessage(envelop);
            return;
        }
        ensureRtcEngineReadyLock();
        mEngineConfig.mVideoDimension = videoDimension;
        if (!TextUtils.isEmpty(encryptionKey)) {
            mRtcEngine.setEncryptionMode(encryptionMode);
            mRtcEngine.setEncryptionSecret(encryptionKey);
        }
        // mRtcEngine.setVideoProfile(mEngineConfig.mVideoProfile, false);  //for sdk earlier than 2.3.0
        mRtcEngine.setVideoEncoderConfiguration(
                new VideoEncoderConfiguration(videoDimension, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                                              VideoEncoderConfiguration.STANDARD_BITRATE,
                                              VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
        Log.d(TAG, "configEngine " + mEngineConfig.mVideoDimension + " " + encryptionMode);
    }

    public final void preview(boolean start, SurfaceView view, int uid) {
        if (Thread.currentThread() != this) {
            Log.w(TAG, "preview() - worker thread asynchronously " + start + " " + view + " " + (uid & 0XFFFFFFFFL));
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_PREVIEW;
            envelop.obj = new Object[]{start, view, uid};
            mWorkerHandler.sendMessage(envelop);
            return;
        }
        ensureRtcEngineReadyLock();
        if (start) {
            mRtcEngine.setupLocalVideo(new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid));
            mRtcEngine.startPreview();
        } else {
            mRtcEngine.stopPreview();
        }
    }

    public static String getDeviceID(Context context) {
        // XXX according to the API docs, this value may change after factory reset
        // use Android id as device id
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private RtcEngine ensureRtcEngineReadyLock() {
        if (mRtcEngine == null) {
            String appId = mContext.getString(R.string.private_app_id);
            if (TextUtils.isEmpty(appId)) {
                throw new RuntimeException("NEED TO use your App ID, get your own ID at https://dashboard.agora.io/");
            }
            try {
                mRtcEngine = RtcEngine.create(mContext, appId, mEngineEventHandler.mRtcEventHandler);
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
                throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
            }
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            mRtcEngine.enableVideo();
            mRtcEngine.enableAudioVolumeIndication(200, 3); // 200 ms
            mRtcEngine.setParameters("{\"rtc.log_filter\":65535}");
            mRtcEngine.setLogFilter(LOG_FILTER_DEBUG);
            String dir =
                    Environment.getExternalStorageDirectory() + File.separator + mContext.getPackageName() + "/log";
            new File(dir).mkdirs();
            try {
                new File(dir + "/agora-rtc.log").createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRtcEngine.setLogFile(dir + "/agora-rtc.log");
        }
        return mRtcEngine;
    }

    public MyEngineEventHandler eventHandler() {
        return mEngineEventHandler;
    }

    public RtcEngine getRtcEngine() {
        return mRtcEngine;
    }

    /**
     * call this method to exit
     * should ONLY call this method when this thread is running
     */
    public final void exit() {
        if (Thread.currentThread() != this) {
            Log.w(TAG, "exit() - exit app thread asynchronously");
            mWorkerHandler.sendEmptyMessage(ACTION_WORKER_THREAD_QUIT);
            return;
        }
        mReady = false;
        // TODO should remove all pending(read) messages
        Log.d(TAG, "exit() > start");
        // exit thread looper
        Looper.myLooper().quit();
        mWorkerHandler.release();
        Log.d(TAG, "exit() > end");
    }

    public WorkerThread(Context context) {
        this.mContext = context;
        this.mEngineConfig = new EngineConfig();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        this.mEngineConfig.mUid = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_UID, 0);
        this.mEngineEventHandler = new MyEngineEventHandler(mContext, this.mEngineConfig);
    }
}
