package com.netease.signalling.demo.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.netease.signalling.demo.R;

public class VideoUserStatusHolder extends RecyclerView.ViewHolder {

    public final RelativeLayout mMaskView;

    public final ImageView mAvatar;

    public final ImageView mIndicator;

    public final LinearLayout mVideoInfo;

    public final TextView mMetaData;

    public VideoUserStatusHolder(View v) {
        super(v);
        mMaskView = v.findViewById(R.id.user_control_mask);
        mAvatar = v.findViewById(R.id.default_avatar);
        mIndicator = v.findViewById(R.id.indicator);
        mVideoInfo = v.findViewById(R.id.video_info_container);
        mMetaData = v.findViewById(R.id.video_info_metadata);
    }
}
