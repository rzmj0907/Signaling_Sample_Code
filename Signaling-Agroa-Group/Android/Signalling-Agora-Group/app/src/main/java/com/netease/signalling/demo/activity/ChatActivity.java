package com.netease.signalling.demo.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.avsignalling.SignallingService;
import com.netease.nimlib.sdk.avsignalling.builder.InviteParamBuilder;
import com.netease.nimlib.sdk.avsignalling.model.ChannelBaseInfo;
import com.netease.signalling.demo.R;
import com.netease.signalling.demo.model.AGEventHandler;
import com.netease.signalling.demo.model.Constant;
import com.netease.signalling.demo.model.ConstantApp;
import com.netease.signalling.demo.model.UserStatusData;
import com.netease.signalling.demo.model.VideoInfoData;
import com.netease.signalling.demo.ui.GridVideoViewContainer;
import com.netease.signalling.demo.ui.RtlLinearLayoutManager;
import com.netease.signalling.demo.ui.SmallVideoViewAdapter;
import com.netease.signalling.demo.ui.SmallVideoViewDecoration;
import com.netease.signalling.demo.ui.VideoViewEventListener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class ChatActivity extends BaseActivity implements AGEventHandler {

    public static final int LAYOUT_TYPE_DEFAULT = 0;

    public static final int LAYOUT_TYPE_SMALL = 1;

    // should only be modified under UI thread
    private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>(); // uid = 0 || uid == EngineConfig.mUid

    public int mLayoutType = LAYOUT_TYPE_DEFAULT;

    private GridVideoViewContainer mGridVideoViewContainer;

    private RelativeLayout mSmallVideoViewDock;

    private volatile boolean mVideoMuted = false;

    private volatile boolean mAudioMuted = false;

    private volatile int mAudioRouting = -1; // Default

    private boolean mIsLandscape = false;

    private SmallVideoViewAdapter mSmallVideoViewAdapter;

    private EditText inviteBox;

    private ChannelBaseInfo channelBaseInfo;
    //private String channelName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        if (!checkSelfPermissions()) {
            showToast("权限不足, 请打开相应权限");
            finish();
            return;
        }
        findViews();
    }
    private void findViews() {
        mGridVideoViewContainer = findViewById(R.id.grid_video_view_container);
        inviteBox = findViewById(R.id.invite_user_account);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    protected void initUIandEvent() {
        event().addEventHandler(this);
        Intent i = getIntent();
        channelBaseInfo = (ChannelBaseInfo) i.getSerializableExtra(ConstantApp.ACTION_KEY_CHANNEL_INFO);
        if (channelBaseInfo == null) {
            showToast("数据错误，请重试");
            finish();
            return;
        }
        final String encryptionKey = getIntent().getStringExtra(ConstantApp.ACTION_KEY_ENCRYPTION_KEY);
        final String encryptionMode = getIntent().getStringExtra(ConstantApp.ACTION_KEY_ENCRYPTION_MODE);
        doConfigEngine(encryptionKey, encryptionMode);
        mGridVideoViewContainer.setItemEventHandler(new VideoViewEventListener() {

            @Override
            public void onItemDoubleClick(View v, Object item) {
                Log.d(TAG, "onItemDoubleClick " + v + " " + item + " " + mLayoutType);
                if (mUidsList.size() < 2) {
                    return;
                }
                UserStatusData user = (UserStatusData) item;
                int uid = (user.mUid == 0) ? config().mUid : user.mUid;
                if (mLayoutType == LAYOUT_TYPE_DEFAULT && mUidsList.size() != 1) {
                    switchToSmallVideoView(uid);
                } else {
                    switchToDefaultVideoView();
                }
            }
        });
        SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
        rtcEngine().setupLocalVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        surfaceV.setZOrderOnTop(false);
        surfaceV.setZOrderMediaOverlay(false);
        mUidsList.put(0, surfaceV); // get first surface view
        mGridVideoViewContainer.initViewContainer(this, 0, mUidsList, mIsLandscape); // first is now full view
        worker().preview(true, surfaceV, 0);
        worker().joinChannel(channelBaseInfo.getChannelName(), config().mUid);
        TextView textChannelName = findViewById(R.id.channel_name);
        textChannelName.setText(channelBaseInfo.getChannelName());
        optional();
    }

    private void optional() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    private void optionalDestroy() {
    }

    private int getVideoProfileIndex() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int profileIndex = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX,
                                       ConstantApp.DEFAULT_PROFILE_IDX);
        if (profileIndex > ConstantApp.VIDEO_DIMENSIONS.length - 1) {
            profileIndex = ConstantApp.DEFAULT_PROFILE_IDX;
            // save the new value
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, profileIndex);
            editor.apply();
        }
        return profileIndex;
    }

    private void doConfigEngine(String encryptionKey, String encryptionMode) {
        VideoEncoderConfiguration.VideoDimensions videoDimension = ConstantApp.VIDEO_DIMENSIONS[getVideoProfileIndex()];
        worker().configEngine(videoDimension, encryptionKey, encryptionMode);
    }

    public void onInviteClicked(View view) {
        String account = inviteBox.getText().toString();
        if (TextUtils.isEmpty(account)) {
            showToast("邀请账号不能为空");
            return;
        }
        InviteParamBuilder inviteParamBuilder = new InviteParamBuilder(channelBaseInfo.getChannelId(), account,
                                                                       UUID.randomUUID().toString());
        NIMClient.getService(SignallingService.class).invite(inviteParamBuilder).setCallback(
                new RequestCallbackWrapper<Void>() {

                    @Override
                    public void onResult(int i, Void aVoid, Throwable throwable) {
                        closeIME(inviteBox);
                        if (i == ResponseCode.RES_SUCCESS) {
                            showToast("邀请成功");
                        } else {
                            showToast("账号不存在，或其他错误");
                        }
                    }
                });
    }

    @Override
    protected void deInitUIandEvent() {
        optionalDestroy();
        doLeaveChannel();
        event().removeEventHandler(this);
        if (mUidsList.size() == 1) {// 只有自己一个人了，退出关闭房间
            NIMClient.getService(SignallingService.class).close(channelBaseInfo.getChannelId(), false, "");
        }else{
            NIMClient.getService(SignallingService.class).leave(channelBaseInfo.getChannelId(), false, "");
        }
        mUidsList.clear();
    }

    private void doLeaveChannel() {
        worker().leaveChannel(config().mChannel);
        worker().preview(false, null, 0);
    }

    public void onEndCallClicked(View view) {
        Log.i(TAG, "onEndCallClicked " + view);
        finish();
    }

    public void onVoiceChatClicked(View view) {
        Log.i(TAG, "onVoiceChatClicked " + view + " " + mUidsList.size() + " video_status: " + mVideoMuted +
                   " audio_status: " + mAudioMuted);
        if (mUidsList.size() == 0) {
            return;
        }
        SurfaceView surfaceV = getLocalView();
        if (surfaceV == null || surfaceV.getParent() == null) {
            Log.w(TAG, "onVoiceChatClicked " + view + " " + surfaceV);
            return;
        }
        RtcEngine rtcEngine = rtcEngine();
        mVideoMuted = !mVideoMuted;
        if (mVideoMuted) {
            rtcEngine.disableVideo();
        } else {
            rtcEngine.enableVideo();
        }
        ImageView iv = (ImageView) view;
        iv.setImageResource(mVideoMuted ? R.drawable.btn_video : R.drawable.btn_voice);
        hideLocalView(mVideoMuted);
    }

    private SurfaceView getLocalView() {
        for (HashMap.Entry<Integer, SurfaceView> entry : mUidsList.entrySet()) {
            if (entry.getKey() == 0 || entry.getKey() == config().mUid) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void hideLocalView(boolean hide) {
        int uid = config().mUid;
        doHideTargetView(uid, hide);
    }

    private void doHideTargetView(int targetUid, boolean hide) {
        HashMap<Integer, Integer> status = new HashMap<>();
        status.put(targetUid, hide ? UserStatusData.VIDEO_MUTED : UserStatusData.DEFAULT_STATUS);
        if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
            mGridVideoViewContainer.notifyUiChanged(mUidsList, targetUid, status, null);
        } else if (mLayoutType == LAYOUT_TYPE_SMALL) {
            UserStatusData bigBgUser = mGridVideoViewContainer.getItem(0);
            if (bigBgUser.mUid == targetUid) { // big background is target view
                mGridVideoViewContainer.notifyUiChanged(mUidsList, targetUid, status, null);
            } else { // find target view in small video view list
                Log.w(TAG,
                      "SmallVideoViewAdapter call notifyUiChanged " + mUidsList + " " + (bigBgUser.mUid & 0xFFFFFFFFL) +
                      " target: " + (targetUid & 0xFFFFFFFFL) + "==" + targetUid + " " + status);
                mSmallVideoViewAdapter.notifyUiChanged(mUidsList, bigBgUser.mUid, status, null);
            }
        }
    }

    public void onVoiceMuteClicked(View view) {
        Log.i(TAG, "onVoiceMuteClicked " + view + " " + mUidsList.size() + " video_status: " + mVideoMuted +
                   " audio_status: " + mAudioMuted);
        if (mUidsList.size() == 0) {
            return;
        }
        RtcEngine rtcEngine = rtcEngine();
        rtcEngine.muteLocalAudioStream(mAudioMuted = !mAudioMuted);
        ImageView iv = (ImageView) view;
        if (mAudioMuted) {
            iv.setColorFilter(getResources().getColor(R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
        } else {
            iv.clearColorFilter();
        }
    }

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        doRenderRemoteUi(uid);
    }

    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                if (mUidsList.containsKey(uid)) {
                    return;
                }
                SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
                mUidsList.put(uid, surfaceV);
                boolean useDefaultLayout = mLayoutType == LAYOUT_TYPE_DEFAULT;
                surfaceV.setZOrderOnTop(true);
                surfaceV.setZOrderMediaOverlay(true);
                rtcEngine().setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                if (useDefaultLayout) {
                    Log.d(TAG, "doRenderRemoteUi LAYOUT_TYPE_DEFAULT " + (uid & 0xFFFFFFFFL));
                    switchToDefaultVideoView();
                } else {
                    int bigBgUid = mSmallVideoViewAdapter == null ? uid : mSmallVideoViewAdapter.getExceptedUid();
                    Log.d(TAG,
                          "doRenderRemoteUi LAYOUT_TYPE_SMALL " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL));
                    switchToSmallVideoView(bigBgUid);
                }
            }
        });
    }

    @Override
    public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
        Log.d(TAG, "onJoinChannelSuccess " + channel + " " + (uid & 0xFFFFFFFFL) + " " + elapsed);
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                SurfaceView local = mUidsList.remove(0);
                if (local == null) {
                    return;
                }
                mUidsList.put(uid, local);
            }
        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        doRemoveRemoteUi(uid);
    }

    @Override
    public void onExtraCallback(final int type, final Object... data) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                doHandleExtraCallback(type, data);
            }
        });
    }

    private void doHandleExtraCallback(int type, Object... data) {
        int peerUid;
        boolean muted;
        switch (type) {
            case AGEventHandler.EVENT_TYPE_ON_USER_AUDIO_MUTED:
                peerUid = (Integer) data[0];
                muted = (boolean) data[1];
                if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
                    HashMap<Integer, Integer> status = new HashMap<>();
                    status.put(peerUid, muted ? UserStatusData.AUDIO_MUTED : UserStatusData.DEFAULT_STATUS);
                    mGridVideoViewContainer.notifyUiChanged(mUidsList, config().mUid, status, null);
                }
                break;
            case AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_MUTED:
                peerUid = (Integer) data[0];
                muted = (boolean) data[1];
                doHideTargetView(peerUid, muted);
                break;
            case AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_STATS:
                IRtcEngineEventHandler.RemoteVideoStats stats = (IRtcEngineEventHandler.RemoteVideoStats) data[0];
                if (Constant.SHOW_VIDEO_INFO) {
                    if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
                        mGridVideoViewContainer.addVideoInfo(stats.uid,
                                                             new VideoInfoData(stats.width, stats.height, stats.delay,
                                                                               stats.receivedFrameRate,
                                                                               stats.receivedBitrate));
                        int uid = config().mUid;
                        int profileIndex = getVideoProfileIndex();
                        String resolution = getResources().getStringArray(
                                R.array.string_array_resolutions)[profileIndex];
                        String fps = getResources().getStringArray(R.array.string_array_frame_rate)[profileIndex];
                        String bitrate = getResources().getStringArray(R.array.string_array_bit_rate)[profileIndex];
                        String[] rwh = resolution.split("x");
                        int width = Integer.valueOf(rwh[0]);
                        int height = Integer.valueOf(rwh[1]);
                        mGridVideoViewContainer.addVideoInfo(uid, new VideoInfoData(width > height ? width : height,
                                                                                    width > height ? height : width, 0,
                                                                                    Integer.valueOf(fps),
                                                                                    Integer.valueOf(bitrate)));
                    }
                } else {
                    mGridVideoViewContainer.cleanVideoInfo();
                }
                break;
            case AGEventHandler.EVENT_TYPE_ON_SPEAKER_STATS:
                IRtcEngineEventHandler.AudioVolumeInfo[] infos = (IRtcEngineEventHandler.AudioVolumeInfo[]) data[0];
                if (infos.length == 1 && infos[0].uid == 0) { // local guy, ignore it
                    break;
                }
                if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
                    HashMap<Integer, Integer> volume = new HashMap<>();
                    for (IRtcEngineEventHandler.AudioVolumeInfo each : infos) {
                        peerUid = each.uid;
                        int peerVolume = each.volume;
                        if (peerUid == 0) {
                            continue;
                        }
                        volume.put(peerUid, peerVolume);
                    }
                    mGridVideoViewContainer.notifyUiChanged(mUidsList, config().mUid, null, volume);
                }
                break;
            case AGEventHandler.EVENT_TYPE_ON_APP_ERROR:
                int subType = (int) data[0];
                if (subType == ConstantApp.AppError.NO_NETWORK_CONNECTION) {
                    showLongToast(getString(R.string.msg_no_network_connection));
                }
                break;
            //            case AGEventHandler.EVENT_TYPE_ON_DATA_CHANNEL_MSG:
            //                peerUid = (Integer) data[0];
            //                final byte[] content = (byte[]) data[1];
            //                notifyMessageChanged(new Message(new User(peerUid, String.valueOf(peerUid)), new String(content)));
            //                break;
            //            case AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR: {
            //                int error = (int) data[0];
            //                String description = (String) data[1];
            //                notifyMessageChanged(new Message(new User(0, null), error + " " + description));
            //                break;
            //            }
            case AGEventHandler.EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED:
                notifyHeadsetPlugged((int) data[0]);
                break;

        }
    }

    private void requestRemoteStreamType(final int currentHostCount) {
        Log.d(TAG, "requestRemoteStreamType " + currentHostCount);
    }

    private void doRemoveRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                Object target = mUidsList.remove(uid);
                if (target == null) {
                    return;
                }
                int bigBgUid = -1;
                if (mSmallVideoViewAdapter != null) {
                    bigBgUid = mSmallVideoViewAdapter.getExceptedUid();
                }
                Log.d(TAG,
                      "doRemoveRemoteUi " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL) + " " + mLayoutType);
                if (mLayoutType == LAYOUT_TYPE_DEFAULT || uid == bigBgUid) {
                    switchToDefaultVideoView();
                } else {
                    switchToSmallVideoView(bigBgUid);
                }
            }
        });
    }

    private void switchToDefaultVideoView() {
        if (mSmallVideoViewDock != null) {
            mSmallVideoViewDock.setVisibility(View.GONE);
        }
        mGridVideoViewContainer.initViewContainer(this, config().mUid, mUidsList, mIsLandscape);
        mLayoutType = LAYOUT_TYPE_DEFAULT;
        boolean setRemoteUserPriorityFlag = false;
        int sizeLimit = mUidsList.size();
        if (sizeLimit > ConstantApp.MAX_PEER_COUNT + 1) {
            sizeLimit = ConstantApp.MAX_PEER_COUNT + 1;
        }
        for (int i = 0; i < sizeLimit; i++) {
            int uid = mGridVideoViewContainer.getItem(i).mUid;
            if (config().mUid != uid) {
                if (!setRemoteUserPriorityFlag) {
                    setRemoteUserPriorityFlag = true;
                    rtcEngine().setRemoteUserPriority(uid, Constants.USER_PRIORITY_HIGH);
                    Log.d(TAG,
                          "setRemoteUserPriority USER_PRIORITY_HIGH " + mUidsList.size() + " " + (uid & 0xFFFFFFFFL));
                } else {
                    rtcEngine().setRemoteUserPriority(uid, Constants.USER_PRIORITY_NORANL);
                    Log.d(TAG,
                          "setRemoteUserPriority USER_PRIORITY_NORANL " + mUidsList.size() + " " + (uid & 0xFFFFFFFFL));
                }
            }
        }
    }

    private void switchToSmallVideoView(int bigBgUid) {
        HashMap<Integer, SurfaceView> slice = new HashMap<>(1);
        slice.put(bigBgUid, mUidsList.get(bigBgUid));
        Iterator<SurfaceView> iterator = mUidsList.values().iterator();
        while (iterator.hasNext()) {
            SurfaceView s = iterator.next();
            s.setZOrderOnTop(true);
            s.setZOrderMediaOverlay(true);
        }
        mUidsList.get(bigBgUid).setZOrderOnTop(false);
        mUidsList.get(bigBgUid).setZOrderMediaOverlay(false);
        mGridVideoViewContainer.initViewContainer(this, bigBgUid, slice, mIsLandscape);
        bindToSmallVideoView(bigBgUid);
        mLayoutType = LAYOUT_TYPE_SMALL;
        requestRemoteStreamType(mUidsList.size());
    }

    private void bindToSmallVideoView(int exceptUid) {
        if (mSmallVideoViewDock == null) {
            ViewStub stub = findViewById(R.id.small_video_view_dock);
            mSmallVideoViewDock = (RelativeLayout) stub.inflate();
        }
        boolean twoWayVideoCall = mUidsList.size() == 2;
        RecyclerView recycler = findViewById(R.id.small_video_view_container);
        boolean create = false;
        if (mSmallVideoViewAdapter == null) {
            create = true;
            mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, config().mUid, exceptUid, mUidsList,
                                                               new VideoViewEventListener() {

                                                                   @Override
                                                                   public void onItemDoubleClick(View v, Object item) {
                                                                       switchToDefaultVideoView();
                                                                   }
                                                               });
            mSmallVideoViewAdapter.setHasStableIds(true);
        }
        recycler.setHasFixedSize(true);
        Log.d(TAG, "bindToSmallVideoView " + twoWayVideoCall + " " + (exceptUid & 0xFFFFFFFFL));
        if (twoWayVideoCall) {
            recycler.setLayoutManager(
                    new RtlLinearLayoutManager(getApplicationContext(), RtlLinearLayoutManager.HORIZONTAL, false));
        } else {
            recycler.setLayoutManager(
                    new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        }
        recycler.addItemDecoration(new SmallVideoViewDecoration());
        recycler.setAdapter(mSmallVideoViewAdapter);
        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);
        if (!create) {
            mSmallVideoViewAdapter.setLocalUid(config().mUid);
            mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null);
        }
        for (Integer tempUid : mUidsList.keySet()) {
            if (config().mUid != tempUid) {
                if (tempUid == exceptUid) {
                    rtcEngine().setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_HIGH);
                    Log.d(TAG, "setRemoteUserPriority USER_PRIORITY_HIGH " + mUidsList.size() + " " +
                               (tempUid & 0xFFFFFFFFL));
                } else {
                    rtcEngine().setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_NORANL);
                    Log.d(TAG, "setRemoteUserPriority USER_PRIORITY_NORANL " + mUidsList.size() + " " +
                               (tempUid & 0xFFFFFFFFL));
                }
            }
        }
        recycler.setVisibility(View.VISIBLE);
        mSmallVideoViewDock.setVisibility(View.VISIBLE);
    }

    public void notifyHeadsetPlugged(final int routing) {
        Log.i(TAG, "notifyHeadsetPlugged " + routing + " " + mVideoMuted);
        mAudioRouting = routing;
        if (!mVideoMuted) {
            return;
        }
        ImageView iv = findViewById(R.id.customized_function_id);
        if (mAudioRouting == 3) { // Speakerphone
            iv.setColorFilter(getResources().getColor(R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
        } else {
            iv.clearColorFilter();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mIsLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
            switchToDefaultVideoView();
        } else if (mSmallVideoViewAdapter != null) {
            switchToSmallVideoView(mSmallVideoViewAdapter.getExceptedUid());
        }
    }
}
