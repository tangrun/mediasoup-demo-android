package com.tangrun.mschat.ui;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.R;
import com.tangrun.mschat.databinding.MsFragmentSingleCallBinding;
import com.tangrun.mschat.databinding.MsLayoutActionBinding;
import com.tangrun.mschat.model.BuddyModel;
import com.tangrun.mschat.model.IBuddyModelObserver;
import com.tangrun.mschat.model.UIRoomStore;
import com.tangrun.mschat.view.InitSurfaceViewRender;
import com.tangrun.mslib.enums.*;
import com.tangrun.mslib.lv.ChangedMutableLiveData;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import org.jetbrains.annotations.NotNull;
import org.webrtc.VideoTrack;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 9:36
 */
public class SingleCallRoomFragment extends Fragment {
    private static final String TAG = "MS_UI_SingleCall";

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
    Boolean mimeShowFullRenderActual = null;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = MsFragmentSingleCallBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        ImmersionBar.with(this)
                .hideBar(BarHide.FLAG_HIDE_BAR)
                .fullScreen(true)
                .statusBarDarkFont(false)
                .init();
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        uiRoomStore = MSManager.getCurrent();

        uiRoomStore.mine.observe(this, buddyModel -> {
            Log.d(TAG, "mime get : " + buddyModel);
            buddyModel.videoTrack.observe(this, videoTrack -> {
                resetRenderBinding();
            });
        });
        mimeShowFullRender.observe(this, aBoolean -> {
            resetRenderBinding();
        });
        target.observe(this, buddyModel -> {
            if (buddyModel == null) return;
            // 仅在初始化时调用
            buddyModel.videoTrack.observe(this, videoTrack -> {
                resetRenderBinding();
            });
            binding.msTvUserName.setText(buddyModel.buddy.getDisplayName());
            Glide.with(binding.msIvUserAvatar).load(buddyModel.buddy.getAvatar())
                    .apply(new RequestOptions()
                            .error(R.drawable.ms_default_avatar)
                            .placeholder(R.drawable.ms_default_avatar))
                    .into(binding.msIvUserAvatar);
            buddyModel.state.observe(SingleCallRoomFragment.this, unused -> {
                setTipText();
            });
        });

        // 重新打开界面时 人已经进入过就不会重新回调client的接口回调 所以buddyObservable就失效了 需要从list里找
        for (BuddyModel buddyModel : uiRoomStore.buddyModels) {
            if (!buddyModel.buddy.isProducer() && target.getValue() == null) {
                target.applySet(buddyModel);
                break;
            }
        }
        uiRoomStore.buddyObservable.registerObserver(new IBuddyModelObserver() {
            @Override
            public void onBuddyAdd(int position, BuddyModel buddyModel) {
                if (!buddyModel.buddy.isProducer() && target.getValue() == null) {
                    target.applySet(buddyModel);
                }
            }

            @Override
            public void onBuddyRemove(int position, BuddyModel buddyModel) {
                if (buddyModel == target.getValue()) {
                    target.applySet(null);
                }
            }
        });

        binding.msVRendererWindow.setZOrderOnTop(true);
        binding.msVRendererFull.setZOrderOnTop(false);

        binding.msVRendererWindow.setOnClickListener(v -> {
            mimeShowFullRender.applySet(!mimeShowFullRender.getValue());
        });
        binding.msIvMinimize.setOnClickListener(v -> {
            uiRoomStore.onMinimize(getActivity());
        });
        binding.msRoot.setOnClickListener(v -> {
            if (uiRoomStore.callingActual.getValue() != Boolean.TRUE || uiRoomStore.audioOnly) return;
            showUI(!showUI);
        });
        // 通话时间
        uiRoomStore.callTime.observe(this, s -> {
            if (s == null) binding.msTvTime.setVisibility(View.GONE);
            binding.msTvTime.setText(s);
        });
        uiRoomStore.callingActual.observe(this, aBoolean -> {
            // 状态提示 开使通话用状态不能判断出来 所以新加calling
            if (aBoolean) setTipText();
        });
        uiRoomStore.localState.observeForever(localState -> {
            Log.d(TAG, "onViewCreated: " + localState);
            // 状态提示
            setTipText();
            showUI(showUI);

            // action
            hideAllAction();
            if (localState.second == ConversationState.Invited) {
                // 接听/挂断
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomLeft);
                uiRoomStore.Action_JoinAction.bindView(binding.llActionBottomRight);
            } else if (localState.second == ConversationState.Joined
                    || (localState.second == ConversationState.New && localState.first == LocalConnectState.JOINED)) {
                if (uiRoomStore.audioOnly) {
                    // 麦克风/挂断/扬声器
                    uiRoomStore.Action_SpeakerOn.bindView(binding.llActionBottomLeft);
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                    uiRoomStore.Action_MicrophoneDisabled.bindView(binding.llActionBottomRight);
                } else {
                    // 麦克风/摄像头/切换摄像头 挂断
                    uiRoomStore.Action_SpeakerOn.bindView(binding.llActionBottomLeft);
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                    uiRoomStore.Action_CameraNotFacing.bindView(binding.llActionBottomRight);
                }
            } else {
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
            }

            if (binding.llActionTopLeft.llContent.getVisibility() != View.VISIBLE
                    && binding.llActionTopCenter.llContent.getVisibility() != View.VISIBLE
                    && binding.llActionTopRight.llContent.getVisibility() != View.VISIBLE) {
                binding.msLlTop.setVisibility(View.GONE);
            } else {
                binding.msLlTop.setVisibility(View.VISIBLE);
            }
            if (binding.llActionBottomLeft.llContent.getVisibility() != View.VISIBLE
                    && binding.llActionBottomCenter.llContent.getVisibility() != View.VISIBLE
                    && binding.llActionBottomRight.llContent.getVisibility() != View.VISIBLE) {
                binding.msLlBottom.setVisibility(View.GONE);
            } else {
                binding.msLlBottom.setVisibility(View.VISIBLE);
            }
        });

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


    Runnable tipDismissRunnable = () -> {
        binding.msTvTip.setVisibility(View.GONE);
    };
    Observable<Long> remoteNoAnswerTipObservable = Observable.interval(30, 10, TimeUnit.SECONDS);
    Disposable remoteNoAnswerTipConsumerDisposable = null;
    Consumer<Long> remoteNoAnswerTipConsumer = new Consumer<Long>() {
        @Override
        public void accept(Long aLong) throws Exception {
            binding.msTvCenterTip.setText("对方不在线或者手机不在身边");
            binding.msTvCenterTip.setVisibility(View.VISIBLE);
            binding.msTvCenterTip.postDelayed(new Runnable() {
                @Override
                public void run() {
                    binding.msTvCenterTip.setVisibility(View.GONE);
                }
            }, 3000);
        }
    };
    /**
     * 挂断状态下 防止状态覆盖提示改变
     */
    boolean tipIgnoreSet = false;

    private void setTipText() {
        if (tipIgnoreSet) return;
        String tip = null;
        int tipDelayDismissTime = 0;
        boolean callEnded = false;
        boolean inWaitRemoteJoin = false;

        binding.msTvTip.removeCallbacks(tipDismissRunnable);

        Pair<LocalConnectState, ConversationState> localState = uiRoomStore.localState.getValue();
        LocalConnectState localConnectState = localState == null ? null : localState.first;
        ConversationState localConversationState = localState == null ? null : localState.second;
        ConnectionState targetConnectionState = target.getValue() == null ? null : target.getValue().connectionState.getValue();
        ConversationState targetConversationState = target.getValue() == null ? null : target.getValue().conversationState.getValue();
        Log.d(TAG, "setTipText: local " + localConnectState + " " + localConversationState + " target " + targetConnectionState + " " + targetConversationState);
        // 根据优先级设置显示
        // 自己连接时
        if (localConnectState == LocalConnectState.NEW || localConnectState == LocalConnectState.CONNECTING) {
            tip = "连接中...";
        } else if (localConnectState == LocalConnectState.DISCONNECTED || localConnectState == LocalConnectState.RECONNECTING) {
            tip = "重连中...";
        }
        // 对方重连
        if (tip == null) {
            if (targetConnectionState == ConnectionState.Offline) {
                tip = "对方重连中...";
            }
        }
        // 会话状态
        if (tip == null) {
            if (targetConversationState == ConversationState.InviteBusy) {
                tip = "通话结束，对方忙线";
                callEnded = true;
            } else if (targetConversationState == ConversationState.OfflineTimeout) {
                tip = "通话结束，对方重连超时";
                callEnded = true;
            } else if (targetConversationState == ConversationState.InviteReject) {
                tip = "通话结束，对方已拒绝";
                callEnded = true;
            } else if (targetConversationState == ConversationState.InviteTimeout) {
                tip = "通话结束，对方无人接听";
                callEnded = true;
            } else if (targetConversationState == ConversationState.Left) {
                tip = "通话已结束，对方已挂断";
            } else if (targetConversationState == ConversationState.Joined || targetConversationState == ConversationState.Invited) {
                // 邀请界面
                if (localConversationState == ConversationState.New && targetConversationState == ConversationState.Invited) {
                    tip = "等待对方接听..";
                    inWaitRemoteJoin = true;
                }
                // 都join了 通话中
                else if (localConversationState == ConversationState.Joined && targetConversationState == ConversationState.Joined) {
                    // 只有第一次才显示
                    if (uiRoomStore.activityBindCount == 1) {
                        tip = "开始通话...";
                        tipDelayDismissTime = 2000;
                    }
                }
                // 接听界面
                else if (localConversationState == ConversationState.Invited) {
                    tip = "待接听";
                } else if (localConversationState == ConversationState.InviteReject) {
                    tip = "已拒绝";
                    callEnded = true;
                } else if (localConversationState == ConversationState.InviteTimeout) {
                    tip = "未接听";
                    callEnded = true;
                } else if (localConversationState == ConversationState.Left) {
                    tip = "通话已结束";
                    callEnded = true;
                }
            }
        }

        if (inWaitRemoteJoin) {
            if (remoteNoAnswerTipConsumerDisposable == null || remoteNoAnswerTipConsumerDisposable.isDisposed()) {
                remoteNoAnswerTipConsumerDisposable = remoteNoAnswerTipObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(remoteNoAnswerTipConsumer);
            }
        } else {
            if (remoteNoAnswerTipConsumerDisposable != null && !remoteNoAnswerTipConsumerDisposable.isDisposed()) {
                remoteNoAnswerTipConsumerDisposable.dispose();
                remoteNoAnswerTipConsumerDisposable = null;
            }
        }

        if (callEnded) {
            showUI(true);
            tipIgnoreSet = true;
        }
        if (tip != null) {
            binding.msTvTip.setText(tip);
            binding.msTvTip.setVisibility(View.VISIBLE);
        } else {
            binding.msTvTip.setVisibility(View.GONE);
        }
        if (tipDelayDismissTime > 0)
            binding.msTvTip.postDelayed(tipDismissRunnable, tipDelayDismissTime);
    }

    boolean showUI = false;
    Runnable uiDismiss = () -> {
        showUI(false);
    };

    private void showUI(boolean show) {
        showUI = show;
        binding.msLlUser.removeCallbacks(uiDismiss);
        binding.msIvMinimize.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        binding.msLlUser.setVisibility(show && (uiRoomStore.audioOnly || uiRoomStore.callingActual.getValue() != Boolean.TRUE) ? View.VISIBLE : View.INVISIBLE);
        if (binding.msLlTop.getVisibility() != View.GONE) {
            binding.msLlTop.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
        if (binding.msLlBottom.getVisibility() != View.GONE) {
            binding.msLlBottom.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
        binding.msTvTime.setVisibility(show && uiRoomStore.calling.getValue() == Boolean.TRUE ? View.VISIBLE : View.GONE);
        if (show && uiRoomStore.calling.getValue() == Boolean.TRUE && !uiRoomStore.audioOnly) {
            binding.msLlUser.postDelayed(uiDismiss, 5000);
        }
    }

    private void resetRenderBinding() {
        if (mimeShowFullRender.getValue() == null) return;
        InitSurfaceViewRender targetRender = mimeShowFullRender.getValue() ? binding.msVRendererWindow : binding.msVRendererFull;
        InitSurfaceViewRender mimeRender = mimeShowFullRender.getValue() ? binding.msVRendererFull : binding.msVRendererWindow;
        // 仅有一个视频流时 显示在全屏里
        VideoTrack targetTrack = target.getValue() == null ? null : target.getValue().videoTrack.getValue();
        VideoTrack mimeTrack = uiRoomStore.mine.getValue() == null ? null : uiRoomStore.mine.getValue().videoTrack.getValue();
        if (targetTrack == null && mimeTrack != null) {
            if (mimeRender != binding.msVRendererFull) {
                targetRender = binding.msVRendererWindow;
                mimeRender = binding.msVRendererFull;
            }
        } else if (targetTrack != null && mimeTrack == null) {
            if (mimeRender != binding.msVRendererWindow) {
                targetRender = binding.msVRendererFull;
                mimeRender = binding.msVRendererWindow;
            }
        }

        uiRoomStore.bindBuddyRender(this, target.getValue(), targetRender);
        uiRoomStore.bindBuddyRender(this, uiRoomStore.mine.getValue(), mimeRender);
    }

}
