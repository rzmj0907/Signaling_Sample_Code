package com.netease.signalling.demo.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.avsignalling.SignallingService;
import com.netease.nimlib.sdk.avsignalling.SignallingServiceObserver;
import com.netease.nimlib.sdk.avsignalling.builder.InviteParamBuilder;
import com.netease.nimlib.sdk.avsignalling.constant.ChannelType;
import com.netease.nimlib.sdk.avsignalling.constant.InviteAckStatus;
import com.netease.nimlib.sdk.avsignalling.event.ChannelCommonEvent;
import com.netease.nimlib.sdk.avsignalling.event.InviteAckEvent;
import com.netease.nimlib.sdk.avsignalling.event.InvitedEvent;
import com.netease.nimlib.sdk.avsignalling.model.ChannelBaseInfo;
import com.netease.nimlib.sdk.avsignalling.model.ChannelFullInfo;
import com.netease.signalling.demo.R;
import com.netease.signalling.demo.model.CacheInfo;
import com.netease.signalling.demo.model.ConstantApp;
import com.netease.signalling.demo.utils.ToastHelper;

import java.util.Random;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    public static final int MAX_LENGTH = 8;

    private EditText editAccount;

    private View beInviteContainer;

    private TextView tvBeInviteHint;

    private TextView tvAcceptInvite;

    private TextView tvRejectInvite;

    private long selfUid;

    private InvitedEvent invitedEvent;

    private ChannelBaseInfo channelInfo;

    private Observer<ChannelCommonEvent> onlineObserver = new Observer<ChannelCommonEvent>() {

        @Override
        public void onEvent(ChannelCommonEvent event) {
            onlineEvent(event);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CacheInfo.setBusy(false);
        selfUid = new Random().nextInt();
        setContentView(R.layout.activity_main);
        findViews();
        registerObserver(true);
    }

    @Override
    protected void initUIandEvent() {
    }
    @Override
    protected void deInitUIandEvent() {
        event().removeEventHandler(this);
    }

    @Override
    protected void workThreadInited() {
        // generate self uid  uid 可以不一一对应
        worker().getEngineConfig().mUid = (int) selfUid;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_login_out: {
                NIMClient.getService(AuthService.class).logout();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            case R.id.tv_create: {
                create();
                return;
            }
            case R.id.tv_accept_invite: {
                acceptInvite();
                return;
            }
            case R.id.tv_reject_invite: {
                rejectInvite("任性不想接");
                return;
            }
        }

    }

    @Override
    protected void onDestroy() {
        registerObserver(false);
        super.onDestroy();
    }

    private void findViews() {
        editAccount = findViewById(R.id.edt_call_account);
        beInviteContainer = findViewById(R.id.rl_be_invite_container);
        tvBeInviteHint = findViewById(R.id.tv_be_invite_hint);
        tvAcceptInvite = findViewById(R.id.tv_accept_invite);
        tvRejectInvite = findViewById(R.id.tv_reject_invite);
        findViewById(R.id.tv_login_out).setOnClickListener(this);
        findViewById(R.id.tv_create).setOnClickListener(this);
        tvAcceptInvite.setOnClickListener(this);
        tvRejectInvite.setOnClickListener(this);

    }

    private void registerObserver(boolean register) {
        NIMClient.getService(SignallingServiceObserver.class).observeOnlineNotification(onlineObserver, register);
    }

    protected void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }

        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * 处理在线通知事件
     */
    private void onlineEvent(ChannelCommonEvent event) {
        hideKeyboard(editAccount);
        // 对方应答了你的邀请
        if (event instanceof InviteAckEvent) {
            InviteAckEvent ackEvent = (InviteAckEvent) event;
            if (ackEvent.getAckStatus() == InviteAckStatus.ACCEPT) {
                ToastHelper.showToast(this, "对方同意了你的邀请");
                //goChatting();
            } else {
                ToastHelper.showToast(this, "对方拒绝了你的邀请");
            }
            return;
        }
        //你被别人邀请了
        if (event instanceof InvitedEvent) {
            beInvited(((InvitedEvent) event));
            return;
        }
    }

    private void beInvited(InvitedEvent event) {
        invitedEvent = event;
        if (CacheInfo.isBusy()) {
            ToastHelper.showToast(this, "有人邀请你 ， 已主动拒绝");
            rejectInvite("正忙");
            return;
        }
        beInviteContainer.setVisibility(View.VISIBLE);
        enableClick(true);
        tvBeInviteHint.setText(event.getFromAccountId() + "邀请你加入房间");
    }

    /**
     * 接受对方的的邀请并加入频道
     */
    private void acceptInvite() {
        enableClick(false);
        InviteParamBuilder inviteParam = new InviteParamBuilder(invitedEvent.getChannelBaseInfo().getChannelId(),
                                                                invitedEvent.getFromAccountId(),
                                                                invitedEvent.getRequestId());
        NIMClient.getService(SignallingService.class).acceptInviteAndJoin(inviteParam, selfUid).setCallback(
                new RequestCallbackWrapper<ChannelFullInfo>() {

                    @Override
                    public void onResult(int code, ChannelFullInfo channelFullInfo, Throwable throwable) {
                        enableClick(true);
                        //参考官方文档中关于api以及错误码的说明
                        if (code == ResponseCode.RES_SUCCESS) {
                            ToastHelper.showToast(MainActivity.this, "接收邀请成功");
                            channelInfo = channelFullInfo.getChannelBaseInfo();
                            goChatting();
                        } else {
                            ToastHelper.showToast(MainActivity.this, "接收邀请返回的结果 ， code = " + code +
                                                                     (throwable == null ? "" : ", throwable = " +
                                                                                               throwable.getMessage()));
                        }
                        beInviteContainer.setVisibility(View.GONE);
                    }
                });
    }

    private void enableClick(boolean enable) {
        beInviteContainer.setSelected(enable);
        tvAcceptInvite.setClickable(enable);
        tvRejectInvite.setClickable(enable);
    }

    /**
     * 拒绝对方的邀请
     */
    private void rejectInvite(String customInfo) {
        beInviteContainer.setVisibility(View.GONE);
        enableClick(true);
        InviteParamBuilder inviteParam = new InviteParamBuilder(invitedEvent.getChannelBaseInfo().getChannelId(),
                                                                invitedEvent.getFromAccountId(),
                                                                invitedEvent.getRequestId());
        if (!TextUtils.isEmpty(customInfo)) {
            inviteParam.customInfo(customInfo);
        }
        NIMClient.getService(SignallingService.class).rejectInvite(inviteParam);
    }


    /**
     * 创建频道
     */
    private void create() {
        String roomId = editAccount.getText().toString().trim();
        if (TextUtils.isEmpty(roomId) || roomId.trim().length() > MAX_LENGTH) {
            ToastHelper.showToast(this, "请输入频道账号，最多8位");
            return;
        }
        NIMClient.getService(SignallingService.class).create(ChannelType.VIDEO, roomId, "").setCallback(
                new RequestCallbackWrapper<ChannelBaseInfo>() {

                    @Override
                    public void onResult(int i, ChannelBaseInfo channelBaseInfo, Throwable throwable) {
                        if (i == ResponseCode.RES_SUCCESS) {
                            channelInfo = channelBaseInfo;
                            onJoin();
                            ToastHelper.showToast(MainActivity.this, "创建成功");
                        } else {
                            ToastHelper.showToast(MainActivity.this, "创建返回的结果 ， code = " + i +
                                                                     (throwable == null ? "" : ", throwable = " +
                                                                                               throwable.getMessage()));
                        }
                    }
                });


    }
    private void onJoin() {
        NIMClient.getService(SignallingService.class).join(channelInfo.getChannelId(), selfUid, "", false).setCallback(
                new RequestCallbackWrapper<ChannelFullInfo>() {

                    @Override
                    public void onResult(int i, ChannelFullInfo channelFullInfo, Throwable throwable) {
                        if (i == ResponseCode.RES_SUCCESS) {
                            goChatting();
                            ToastHelper.showToast(MainActivity.this, "加入成功");
                        } else {
                            ToastHelper.showToast(MainActivity.this, "加入返回的结果 ， code = " + i +
                                                                     (throwable == null ? "" : ", throwable = " +
                                                                                               throwable.getMessage()));
                        }
                    }
                });
    }

    private void goChatting() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ConstantApp.ACTION_KEY_CHANNEL_INFO, channelInfo);
        startActivity(intent);
    }
}
