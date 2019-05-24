package com.netease.signalling.demo.model;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;

public class MyEngineEventHandler {
    private static final String TAG = MyEngineEventHandler.class.getSimpleName();

    public MyEngineEventHandler(Context ctx, EngineConfig config) {
        this.mContext = ctx;
        this.mConfig = config;
    }

    private final EngineConfig mConfig;

    private final Context mContext;

    private final ConcurrentHashMap<AGEventHandler, Integer> mEventHandlerList = new ConcurrentHashMap<>();

    public void addEventHandler(AGEventHandler handler) {
        this.mEventHandlerList.put(handler, 0);
    }

    public void removeEventHandler(AGEventHandler handler) {
        this.mEventHandlerList.remove(handler);
    }

    final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
            Log.d(TAG,"onFirstRemoteVideoDecoded " + (uid & 0xFFFFFFFFL) + " " + width + " " + height + " " + elapsed);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
            }
        }

        @Override
        public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
            Log.d(TAG,"onFirstLocalVideoFrame " + width + " " + height + " " + elapsed);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d(TAG,"onUserOffline " + (uid & 0xFFFFFFFFL) + " " + reason);

            // FIXME this callback may return times
            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onUserOffline(uid, reason);
            }
        }

        @Override
        public void onUserMuteVideo(int uid, boolean muted) {
            Log.d(TAG,"onUserMuteVideo " + (uid & 0xFFFFFFFFL) + " " + muted);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_MUTED, uid, muted);
            }
        }

        @Override
        public void onRtcStats(RtcStats stats) {
        }

        @Override
        public void onRemoteVideoStats(RemoteVideoStats stats) {
            Log.d(TAG,"onRemoteVideoStats " + stats.uid + " " + stats.delay + " " + stats.receivedBitrate + " " + stats.receivedFrameRate + " " + stats.width + " " + stats.height);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_STATS, stats);
            }
        }

        @Override
        public void onAudioVolumeIndication(AudioVolumeInfo[] speakerInfos, int totalVolume) {
            if (speakerInfos == null) {
                // quick and dirty fix for crash
                // TODO should reset UI for no sound
                return;
            }

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_SPEAKER_STATS, (Object) speakerInfos);
            }
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {

        }

        @Override
        public void onLastmileQuality(int quality) {
            Log.d(TAG,"onLastmileQuality " + quality);
            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onLastmileQuality(quality);
            }
        }

        @Override
        public void onLastmileProbeResult(IRtcEngineEventHandler.LastmileProbeResult result){
            Log.d(TAG,"onLastmileProbeResult " + result);
            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onLastmileProbeResult(result);
            }
        }
        @Override
        public void onError(int error) {
            Log.d(TAG,"onError " + error + " " + RtcEngine.getErrorDescription(error));

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR, error, RtcEngine
                        .getErrorDescription(error));
            }
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            Log.d(TAG,"onStreamMessage " + (uid & 0xFFFFFFFFL) + " " + streamId + " " + Arrays.toString(data));

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_DATA_CHANNEL_MSG, uid, data);
            }
        }

        public void onStreamMessageError(int uid, int streamId, int error, int missed, int cached) {
            Log.w(TAG,"onStreamMessageError " + (uid & 0xFFFFFFFFL) + " " + streamId + " " + error + " " + missed + " " + cached);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR, error, "on stream msg error " + (uid & 0xFFFFFFFFL) + " " + missed + " " + cached);
            }
        }

        @Override
        public void onConnectionLost() {
            Log.d(TAG,"onConnectionLost");

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_APP_ERROR, ConstantApp.AppError.NO_NETWORK_CONNECTION);
            }
        }

        @Override
        public void onConnectionInterrupted() {
            Log.d(TAG,"onConnectionInterrupted");

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_APP_ERROR, ConstantApp.AppError.NO_NETWORK_CONNECTION);
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(TAG,"onJoinChannelSuccess " + channel + " " + uid + " " + (uid & 0xFFFFFFFFL) + " " + elapsed);

            mConfig.mUid = uid;

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onJoinChannelSuccess(channel, uid, elapsed);
            }
        }

        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(TAG,"onRejoinChannelSuccess " + channel + " " + uid + " " + elapsed);
        }

        @Override
        public void onAudioRouteChanged(int routing) {
            Log.d(TAG,"onAudioRouteChanged " + routing);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED, routing);
            }
        }

        public void onWarning(int warn) {
            Log.d(TAG,"onWarning " + warn);
        }

        @Override
        public void onAudioMixingStateChanged(int state, int errorCode) {
            super.onAudioMixingStateChanged(state, errorCode);
            Log.d(TAG, "onAudioMixingStateChanged() called with: state = [" + state + "], errorCode = [" + errorCode + "]");
        }

        @Override
        public void onAudioMixingFinished() {
            super.onAudioMixingFinished();
            Log.d(TAG, "onAudioMixingFinished() called");
        }
    };

}
