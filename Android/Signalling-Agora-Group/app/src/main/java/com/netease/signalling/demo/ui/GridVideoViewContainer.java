package com.netease.signalling.demo.ui;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.netease.signalling.demo.model.UserStatusData;
import com.netease.signalling.demo.model.VideoInfoData;

import java.util.HashMap;

public class GridVideoViewContainer extends RecyclerView {

    private GridVideoViewContainerAdapter mGridVideoViewContainerAdapter;

    private VideoViewEventListener mEventListener;

    public GridVideoViewContainer(Context context) {
        super(context);
    }

    public GridVideoViewContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GridVideoViewContainer(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setItemEventHandler(VideoViewEventListener listener) {
        this.mEventListener = listener;
    }

    private boolean initAdapter(Activity activity, int localUid, HashMap<Integer, SurfaceView> uids) {
        if (mGridVideoViewContainerAdapter == null) {
            mGridVideoViewContainerAdapter = new GridVideoViewContainerAdapter(activity, localUid, uids,
                                                                               mEventListener);
            mGridVideoViewContainerAdapter.setHasStableIds(true);
            return true;
        }
        return false;
    }

    public void initViewContainer(Activity activity, int localUid, HashMap<Integer, SurfaceView> uids,
                                  boolean isLandscape) {
        boolean newCreated = initAdapter(activity, localUid, uids);
        if (!newCreated) {
            mGridVideoViewContainerAdapter.setLocalUid(localUid);
            mGridVideoViewContainerAdapter.customizedInit(uids, true);
        }
        this.setAdapter(mGridVideoViewContainerAdapter);
        int orientation = isLandscape ? RecyclerView.HORIZONTAL : RecyclerView.VERTICAL;
        int count = uids.size();
        if (count <= 2) { // only local full view or or with one peer
            this.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext(), orientation, false));
        } else if (count > 2) {
            int itemSpanCount = getNearestSqrt(count);
            this.setLayoutManager(
                    new GridLayoutManager(activity.getApplicationContext(), itemSpanCount, orientation, false));
        }
        mGridVideoViewContainerAdapter.notifyDataSetChanged();
    }

    private int getNearestSqrt(int n) {
        return (int) Math.sqrt(n);
    }

    public void notifyUiChanged(HashMap<Integer, SurfaceView> uids, int localUid, HashMap<Integer, Integer> status,
                                HashMap<Integer, Integer> volume) {
        if (mGridVideoViewContainerAdapter == null) {
            return;
        }
        mGridVideoViewContainerAdapter.notifyUiChanged(uids, localUid, status, volume);
    }

    public void addVideoInfo(int uid, VideoInfoData video) {
        if (mGridVideoViewContainerAdapter == null) {
            return;
        }
        mGridVideoViewContainerAdapter.addVideoInfo(uid, video);
    }

    public void cleanVideoInfo() {
        if (mGridVideoViewContainerAdapter == null) {
            return;
        }
        mGridVideoViewContainerAdapter.cleanVideoInfo();
    }

    public UserStatusData getItem(int position) {
        return mGridVideoViewContainerAdapter.getItem(position);
    }

}
