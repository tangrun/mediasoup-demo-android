package com.tangrun.mschat.ui;

import android.os.Bundle;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.mschat.R;
import com.example.mschat.databinding.MsFragmentSingleCallBinding;
import com.example.mschat.databinding.MsLayoutActionBinding;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.model.BuddyModel;
import com.tangrun.mschat.model.IBuddyModelObserver;
import com.tangrun.mschat.model.UIRoomStore;
import org.jetbrains.annotations.NotNull;
import org.mediasoup.droid.lib.enums.CameraFacingState;
import org.mediasoup.droid.lib.enums.ConversationState;
import org.mediasoup.droid.lib.enums.LocalConnectState;
import org.mediasoup.droid.lib.lv.ChangedMutableLiveData;
import org.webrtc.VideoTrack;

import java.util.Arrays;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 9:36
 */
public class SingleCallRoomFragment extends Fragment {
    public static SingleCallRoomFragment newInstance() {

        Bundle args = new Bundle();

        SingleCallRoomFragment fragment = new SingleCallRoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    MsFragmentSingleCallBinding binding;
    ChangedMutableLiveData<BuddyModel> target = new ChangedMutableLiveData<>();
    UIRoomStore uiRoomStore;
    ChangedMutableLiveData<Boolean> mimeShowFullRender = new ChangedMutableLiveData<>(false);

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = MsFragmentSingleCallBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        uiRoomStore = MSManager.getCurrent();

        uiRoomStore.mine.observe(this, buddyModel -> {
            binding.msTvUserName.setText(buddyModel.buddy.getDisplayName());
            Glide.with(binding.msIvUserAvatar).load(buddyModel.buddy.getAvatar())
                    .apply(new RequestOptions()
                            .error(R.drawable.ms_default_avatar)
                            .placeholder(R.drawable.ms_default_avatar))
                    .into(binding.msIvUserAvatar);
            buddyModel.videoTrack.observe(this, videoTrack -> {
                resetRender();
            });
        });
        target.observe(this, buddyModel -> {
            if (buddyModel == null) return;
            buddyModel.videoTrack.observe(this, videoTrack -> {
                resetRender();
            });
        });
        mimeShowFullRender.observe(this, aBoolean -> {
            resetRender();
        });
        uiRoomStore.cameraFacingState.observe(this, cameraFacingState -> {
            if (cameraFacingState == CameraFacingState.inProgress) return;
            resetRender();
        });

        uiRoomStore.buddyObservable.registerObserver(new IBuddyModelObserver() {
            @Override
            public void onBuddyAdd(int position, BuddyModel buddyModel) {
                if (!buddyModel.buddy.isProducer() && target.getValue() == null)
                    target.applySet(buddyModel);
            }

            @Override
            public void onBuddyRemove(int position, BuddyModel buddyModel) {
                if (buddyModel == target.getValue()) {
                    target.applyPost(null);
                }
            }
        });

        binding.msVRendererFull.init(this);
        binding.msVRendererWindow.init(this);

        binding.msVRendererWindow.setOnClickListener(v -> {
            mimeShowFullRender.applySet(!mimeShowFullRender.getValue());
        });
        binding.msIvMinimize.setOnClickListener(v -> {
            uiRoomStore.onMinimize(getActivity());
        });
        binding.msRoot.setOnClickListener(v -> {
            if (uiRoomStore.calling.getValue() != Boolean.TRUE) return;
            showUI(!showUI);
        });
        // 通话时间
        uiRoomStore.callTime.observe(this, s -> {
            if (s == null) binding.msTvTime.setVisibility(View.GONE);
            binding.msTvTime.setText(s);
        });
        uiRoomStore.calling.observe(this, aBoolean -> {
            // 状态提示 开使通话用状态不能判断出来 所以新加calling
            if (aBoolean) setTipText();
        });
        uiRoomStore.localState.observeForever(localState -> {
            // 状态提示
            setTipText();
            showUI(showUI);

            // action
            hideAllAction();
            if (localState.first == LocalConnectState.RECONNECTING
                    || localState.first == LocalConnectState.DISCONNECTED) {
                // 重连中
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
            } else if (localState.first == LocalConnectState.NEW
                    || localState.first == LocalConnectState.CONNECTING) {
                // 连接中
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
            } else {
                if (localState.second == ConversationState.Invited) {
                    // 接听/挂断
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomLeft);
                    uiRoomStore.Action_JoinAction.bindView(binding.llActionBottomRight);
                } else if (localState.second == ConversationState.Joined
                        || (localState.second == ConversationState.New && localState.first == LocalConnectState.JOINED)) {
                    if (uiRoomStore.audioOnly) {
                        // 麦克风/挂断/扬声器
                        uiRoomStore.Action_MicrophoneDisabled.bindView(binding.llActionBottomLeft);
                        uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                        uiRoomStore.Action_SpeakerOn.bindView(binding.llActionBottomRight);
                    } else {
                        // 麦克风/摄像头/切换摄像头 挂断
                        uiRoomStore.Action_MicrophoneDisabled.bindView(binding.llActionTopLeft);
                        uiRoomStore.Action_CameraDisabled.bindView(binding.llActionTopCenter);
                        uiRoomStore.Action_CameraNotFacing.bindView(binding.llActionTopRight);
                        uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                    }
                } else {
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                }
            }
        });

        binding.msVRendererWindow.setZOrderOnTop(true);

        showUI(true);

    }

    public void hideAllAction() {
        for (MsLayoutActionBinding itemActionBinding : Arrays.asList(
                binding.llActionTopLeft,
                binding.llActionTopRight,
                binding.llActionTopCenter,
                binding.llActionBottomLeft,
                binding.llActionBottomRight,
                binding.llActionBottomCenter)) {
            itemActionBinding.llContent.setVisibility(View.INVISIBLE);
        }
    }


    Runnable tipDismiss = () -> {
        binding.msTvTip.setVisibility(View.GONE);
    };

    private void setTipText() {
        String tip = null;
        int delayDismissTime = 0;
        binding.msTvTip.removeCallbacks(tipDismiss);

        Pair<LocalConnectState, ConversationState> localState = uiRoomStore.localState.getValue();

        if (localState != null) {
            if (localState.first == LocalConnectState.NEW || localState.first == LocalConnectState.CONNECTING) {
                tip = "连接中...";
            } else if (localState.first == LocalConnectState.DISCONNECTED || localState.first == LocalConnectState.RECONNECTING) {
                tip = "重连中...";
            } else {
                if (localState.second == ConversationState.New) {
                    tip = "等待对方接听...";
                } else if (localState.second == ConversationState.Invited) {
                    tip = "待接听";
                } else if (localState.second == ConversationState.Joined) {
                    tip = "通话中...";
                    delayDismissTime = 2000;
                } else if (localState.second == ConversationState.InviteBusy
                        || localState.second == ConversationState.Left
                        || localState.second == ConversationState.OfflineTimeout
                        || localState.second == ConversationState.InviteReject
                        || localState.second == ConversationState.InviteTimeout) {
                    tip = "通话已结束";
                    showUI(true);
                }
            }
        }

        if (tip != null) {
            binding.msTvTip.setText(tip);
            binding.msTvTip.setVisibility(View.VISIBLE);
        }
        if (delayDismissTime > 0)
            binding.msTvTip.postDelayed(tipDismiss, delayDismissTime);
    }

    boolean showUI = false;
    Runnable uiDismiss = () -> {
        showUI(false);
    };

    private void showUI(boolean show) {
        showUI = show;
        binding.msLlUser.removeCallbacks(uiDismiss);
        binding.msIvMinimize.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        binding.msLlUser.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        binding.msLlTop.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        binding.msLlBottom.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        binding.msTvTime.setVisibility(show && uiRoomStore.calling.getValue() == Boolean.TRUE ? View.VISIBLE : View.GONE);
        if (show && uiRoomStore.calling.getValue() == Boolean.TRUE && !uiRoomStore.audioOnly) {
            binding.msLlUser.postDelayed(uiDismiss, 5000);
        }
    }

    private void resetRender() {
        if (uiRoomStore.audioOnly) {
            binding.msVRendererWindow.setVisibility(View.GONE);
            binding.msVRendererFull.setVisibility(View.GONE);
            return;
        }
        BuddyModel mime = uiRoomStore.mine.getValue();
        BuddyModel target = this.target.getValue();

        VideoTrack mimeVideoTrack = mime == null ? null : mime.videoTrack.getValue();
        VideoTrack targetVideoTrack = target == null ? null : target.videoTrack.getValue();

        VideoTrack windowRenderTrack = mimeShowFullRender.getValue() ? targetVideoTrack : mimeVideoTrack;
        VideoTrack fullRenderTrack = mimeShowFullRender.getValue() ? mimeVideoTrack : targetVideoTrack;

        if (windowRenderTrack != null && fullRenderTrack == null) {
            fullRenderTrack = windowRenderTrack;
            windowRenderTrack = null;
        }

        binding.msVRendererWindow.bind(this, windowRenderTrack);
        binding.msVRendererWindow.setVisibility(windowRenderTrack == null ? View.GONE : View.VISIBLE);
        binding.msVRendererWindow.setMirror(windowRenderTrack != null && windowRenderTrack == mimeVideoTrack && uiRoomStore.cameraFacingState.getValue() == CameraFacingState.front);

        binding.msVRendererFull.bind(this, fullRenderTrack);
        binding.msVRendererFull.setVisibility(fullRenderTrack == null ? View.GONE : View.VISIBLE);
        binding.msVRendererFull.setMirror(fullRenderTrack != null && fullRenderTrack == mimeVideoTrack && uiRoomStore.cameraFacingState.getValue() == CameraFacingState.front);
    }
}
