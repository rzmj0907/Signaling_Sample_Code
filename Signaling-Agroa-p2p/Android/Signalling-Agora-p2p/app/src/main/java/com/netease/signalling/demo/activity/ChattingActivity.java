package com.netease.signalling.demo.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.avsignalling.SignallingService;
import com.netease.nimlib.sdk.avsignalling.SignallingServiceObserver;
import com.netease.nimlib.sdk.avsignalling.event.ChannelCommonEvent;
import com.netease.nimlib.sdk.avsignalling.event.ControlEvent;
import com.netease.nimlib.sdk.avsignalling.event.UserJoinEvent;
import com.netease.nimlib.sdk.avsignalling.model.ChannelBaseInfo;
import com.netease.nimlib.sdk.avsignalling.model.MemberInfo;
import com.netease.signalling.demo.R;
import com.netease.signalling.demo.model.CacheInfo;
import com.netease.signalling.demo.utils.ToastHelper;

import java.util.ArrayList;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class ChattingActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "ChattingActivity";

    public static final String CHANNEL_INFO = "channel_info";

    private static final int REQUEST_PERMISSIONS = 1001;

    private FrameLayout flRemotePreview;

    private FrameLayout flLocalPreview;

    private View ivMute;

    private View ivCloseCamera;

    private RtcEngine mRtcEngine;

    private ChannelBaseInfo channelInfo;

    private MemberInfo remoteMember;

    private static final String[] ALL_PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                                                 Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                 Manifest.permission.CAMERA,
                                                                 Manifest.permission.RECORD_AUDIO};

    private Observer<ChannelCommonEvent> onlineObserver = new Observer<ChannelCommonEvent>() {

        @Override
        public void onEvent(ChannelCommonEvent event) {
            onlineEvent(event);
        }
    };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    onRemoteUserLeft();
                    finish();
                }
            });
        }

        @Override
        public void onUserMuteVideo(final int uid, final boolean muted) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    onRemoteUserVideoMuted(uid, muted);
                }
            });
        }
    };

    private void onRemoteUserLeft() {
        flRemotePreview.removeAllViews();
    }

    private void onRemoteUserVideoMuted(int uid, boolean muted) {
        SurfaceView surfaceView = (SurfaceView) flRemotePreview.getChildAt(0);
        Object tag = surfaceView.getTag();
        if (tag != null && (Integer) tag == uid) {
            surfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);
        }
    }

    private void setupRemoteVideo(int uid) {
        if (flRemotePreview.getChildCount() >= 1) {
            return;
        }
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        flRemotePreview.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        surfaceView.setTag(uid); // for mark purpose
        if (remoteMember != null) {
            setUInfo(remoteMember.getAccountId() + "\n正在通话中...", flRemotePreview);
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CacheInfo.setBusy(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chationg);
        setupView();
        parseIntent();
        checkAllPermission();
        registerObserver(true);
    }

    private void parseIntent() {
        Intent intent = getIntent();
        channelInfo = (ChannelBaseInfo) intent.getSerializableExtra(CHANNEL_INFO);
    }

    private void setupView() {
        flRemotePreview = findViewById(R.id.fl_remote_render);
        flLocalPreview = findViewById(R.id.fl_local_render);
        ivMute = findViewById(R.id.iv_mute);
        ivCloseCamera = findViewById(R.id.iv_close_camera);
        ivMute.setOnClickListener(this);
        findViewById(R.id.iv_cancel).setOnClickListener(this);
        ivCloseCamera.setOnClickListener(this);
    }


    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void registerObserver(boolean register) {
        //在线通知
        NIMClient.getService(SignallingServiceObserver.class).observeOnlineNotification(onlineObserver, register);
    }


    /**
     * 收到在线通知
     */
    private void onlineEvent(ChannelCommonEvent event) {
        if (event instanceof ControlEvent) {
            String account = event.getFromAccountId();
            String customInfo = event.getCustomInfo();
            ToastHelper.showToast(this, "收到来自于 " + account + " 的控制命令 ，" + customInfo);
        } else if (event instanceof UserJoinEvent) {
            UserJoinEvent event1 = (UserJoinEvent) event;
            remoteMember = event1 != null ? event1.getMemberInfo() : null;
            if (remoteMember != null && flRemotePreview.getChildCount() >= 1) {// already set view
                setUInfo(remoteMember.getAccountId() + "\n正在通话中...", flRemotePreview);
            }
        }
    }
    private void setUInfo(String desc, FrameLayout rootLayout) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                       ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        TextView child = new TextView(this);
        child.setText(desc);
        rootLayout.addView(child, params);
    }

    private void checkAllPermission() {
        ArrayList<String> permissionDenied = new ArrayList<>();
        for (String permission : ALL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                permissionDenied.add(permission);
            }
        }
        if (permissionDenied.size() == 0) {
            allPermissionGranted();
            return;
        }
        String[] deniedArr = new String[0];
        deniedArr = permissionDenied.toArray(deniedArr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(deniedArr, REQUEST_PERMISSIONS);
        }
    }

    private void allPermissionGranted() {
        ToastHelper.showToast(this, " All permission granted ");
        initializeAgoraEngine();
        setupVideoProfile();
        setupLocalVideo();
        joinChannel();
    }

    private void setupVideoProfile() {
        mRtcEngine.enableVideo();
        //      mRtcEngine.setVideoProfile(Constants.VIDEO_PROFILE_360P, false); // Earlier than 2.3.0
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x360,
                                                                              VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                                                                              VideoEncoderConfiguration.STANDARD_BITRATE,
                                                                              VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo() {
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        flLocalPreview.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0));
        setUInfo("我的账号\n" + CacheInfo.getAccount(), flLocalPreview);
    }

    private void joinChannel() {
        mRtcEngine.joinChannel(null, channelInfo.getChannelName(), "Extra Optional Data",
                               0); // if you do not specify the uid, we will generate the uid for you
    }


    @Override
    protected void onPause() {
        mRtcEngine.stopPreview();
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        registerObserver(false);
        shutDown();
        super.onDestroy();
    }

    private void shutDown() {
        CacheInfo.setBusy(false);
        //离开 信令频道
        NIMClient.getService(SignallingService.class).leave(channelInfo.getChannelId(), false, null);
        //离开音视频房间
        mRtcEngine.leaveChannel();
        RtcEngine.destroy();
        NIMClient.getService(SignallingService.class).close(channelInfo.getChannelId(), false, null);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_cancel) {
            finish();
            return;
        }
        // 关闭/打开本地语音发送
        if (id == R.id.iv_mute) {
            boolean isMute = !ivMute.isSelected();
            ImageView iv = (ImageView) ivMute;
            if (iv.isSelected()) {
                iv.setSelected(false);
                iv.clearColorFilter();
            } else {
                iv.setSelected(true);
                iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
            }
            mRtcEngine.muteLocalAudioStream(iv.isSelected());
            NIMClient.getService(SignallingService.class).sendControl(channelInfo.getChannelId(), null,
                                                                      isMute ? "我关闭语音" : "我打开了语音");
            return;
        }
        // 关闭/打开地视频发送
        if (id == R.id.iv_close_camera) {
            boolean isClose = !ivCloseCamera.isSelected();
            ImageView iv = (ImageView) ivCloseCamera;
            if (iv.isSelected()) {
                iv.setSelected(false);
                iv.clearColorFilter();
            } else {
                iv.setSelected(true);
                iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
            }
            mRtcEngine.muteLocalVideoStream(iv.isSelected());
            SurfaceView surfaceView = (SurfaceView) flLocalPreview.getChildAt(0);
            surfaceView.setZOrderMediaOverlay(!iv.isSelected());
            surfaceView.setVisibility(iv.isSelected() ? View.GONE : View.VISIBLE);
            NIMClient.getService(SignallingService.class).sendControl(channelInfo.getChannelId(), null,
                                                                      isClose ? "我关闭视频" : "我打开了视频");
            return;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) {
            return;
        }
        int len = grantResults.length;
        if (len == 0) {
            allPermissionGranted();
            return;
        }
        for (int index = 0; index < len; ++index) {
            if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                ToastHelper.showToast(this, permissions[index] + " 权限获取失败");
            }
        }
    }
}
