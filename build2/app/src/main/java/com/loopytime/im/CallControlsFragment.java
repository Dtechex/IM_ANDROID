/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.loopytime.im;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.loopytime.apprtc.UnhandledExceptionHandler;
import com.loopytime.utils.Constants;

/**
 * Fragment for call control.
 */
public class CallControlsFragment extends Fragment {

    private static final String TAG = CallControlsFragment.class.getSimpleName();
    private ImageView declineCall, muteCall, speakerCall, callImage;
    private LinearLayout optionsLay;
    private RelativeLayout acceptLay;
    private LottieAnimationView callBg;
    private OnCallEvents callEvents;
    private boolean enableMic;
    private boolean enableSpeaker;
    private CallActivity activity;
    Bundle bundle;
    private String from = "", type = "";

    public void setContext(CallActivity activity) {
        this.activity = activity;
    }

    /**
     * Call control interface for container activity.
     */
    public interface OnCallEvents {

        void onCallAccepted();

        void onCallHangUp();

        boolean onToggleMic();

        boolean onToggleSpeaker();

        void onSwitchCamera();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(activity));
        bundle = getArguments();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View controlView = inflater.inflate(R.layout.fragment_call, container, false);

        // Create UI controls.
        callImage = controlView.findViewById(R.id.callImage);
        declineCall = controlView.findViewById(R.id.declineCall);
        muteCall = controlView.findViewById(R.id.muteCall);
        speakerCall = controlView.findViewById(R.id.speakerCall);
        optionsLay = controlView.findViewById(R.id.optionsLay);
        acceptLay = controlView.findViewById(R.id.acceptLay);
        callBg = controlView.findViewById(R.id.callBg);

        from = bundle.getString(Constants.TAG_FROM);
        type = bundle.getString(Constants.TAG_TYPE);
        initView();

        return controlView;
    }

    private void initView() {
        if (from.equals("receive")) {
            acceptLay.setVisibility(View.VISIBLE);
            optionsLay.setVisibility(View.GONE);
        } else {
            acceptLay.setVisibility(View.GONE);
            optionsLay.setVisibility(View.VISIBLE);
        }

        enableMic = activity.micEnabled;
        setMicroPhone(enableMic);
        if (type.equals(Constants.TAG_AUDIO)) {
            enableSpeaker = activity.speaker;
            setEnableSpeaker(enableSpeaker);
        } else {
            speakerCall.setBackground(getResources().getDrawable(R.drawable.white_round_opacity));
            speakerCall.setImageResource(R.drawable.change_camera);
        }
        // Add buttons click events.
        callImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptLay.setVisibility(View.GONE);
                optionsLay.setVisibility(View.VISIBLE);
                callEvents.onCallAccepted();
            }
        });

        declineCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callEvents.onCallHangUp();
            }
        });

        muteCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableMic = callEvents.onToggleMic();
                setMicroPhone(enableMic);
            }
        });

        speakerCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (type.equals(Constants.TAG_AUDIO)) {
                    enableSpeaker = callEvents.onToggleSpeaker();
                    setEnableSpeaker(enableSpeaker);
                } else {
                    callEvents.onSwitchCamera();
                }
            }
        });

    }

    private void setEnableSpeaker(boolean enableSpeaker) {
        if (enableSpeaker) {
            speakerCall.setBackground(getResources().getDrawable(R.drawable.white_round_solid));
            speakerCall.setImageResource(R.drawable.speaker_gray);
        } else {
            speakerCall.setImageResource(R.drawable.speaker);
            speakerCall.setBackground(getResources().getDrawable(R.drawable.white_round_opacity));
        }
    }

    private void setMicroPhone(boolean enableAudio) {
        if (enableAudio) {
            muteCall.setBackground(getResources().getDrawable(R.drawable.white_round_opacity));
            muteCall.setImageResource(R.drawable.mute);
        } else {
            muteCall.setBackground(getResources().getDrawable(R.drawable.white_round_solid));
            muteCall.setImageResource(R.drawable.mute_gray);
        }
    }

    // TODO(sakal): Replace with onAttach(Context) once we only support API level 23+.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callEvents = (OnCallEvents) activity;
    }
}
