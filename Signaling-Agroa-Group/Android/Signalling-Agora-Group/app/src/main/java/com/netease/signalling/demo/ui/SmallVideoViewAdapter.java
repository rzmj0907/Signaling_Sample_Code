package com.netease.signalling.demo.ui;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.HashMap;

public class SmallVideoViewAdapter extends VideoViewAdapter {

    private static final String TAG = "SmallVideoViewAdapter";

    private int mExceptedUid;

    public SmallVideoViewAdapter(Activity activity, int localUid, int exceptedUid, HashMap<Integer, SurfaceView> uids, VideoViewEventListener listener) {
        super(activity, localUid, uids, listener);
        mExceptedUid = exceptedUid;
        Log.d(TAG, "SmallVideoViewAdapter " + (mLocalUid & 0xFFFFFFFFL) + " " + (mExceptedUid & 0xFFFFFFFFL));
    }

    @Override
    protected void customizedInit(HashMap<Integer, SurfaceView> uids, boolean force) {
        VideoViewAdapterUtil.composeDataItem(mUsers, uids, mLocalUid, null, null, mVideoInfo, mExceptedUid);

        if (force || mItemWidth == 0 || mItemHeight == 0) {
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(outMetrics);
            mItemWidth = outMetrics.widthPixels / 4;
            mItemHeight = outMetrics.heightPixels / 4;
        }
    }

    @Override
    public void notifyUiChanged(HashMap<Integer, SurfaceView> uids, int uidExcepted, HashMap<Integer, Integer> status, HashMap<Integer, Integer> volume) {
        mUsers.clear();

        mExceptedUid = uidExcepted;

        Log.d(TAG,"notifyUiChanged " + (mLocalUid & 0xFFFFFFFFL) + " " + (uidExcepted & 0xFFFFFFFFL) + " " + uids + " " + status + " " + volume);
        VideoViewAdapterUtil.composeDataItem(mUsers, uids, mLocalUid, status, volume, mVideoInfo, uidExcepted);

        notifyDataSetChanged();
    }

    public int getExceptedUid() {
        return mExceptedUid;
    }
}
