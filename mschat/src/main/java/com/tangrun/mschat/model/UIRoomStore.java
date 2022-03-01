package com.tangrun.mschat.model;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

import android.util.Pair;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.*;

import androidx.lifecycle.Observer;
import com.example.mschat.R;
import com.example.mschat.databinding.MsLayoutActionBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.tangrun.mschat.UICallback;
import com.tangrun.mschat.enums.CallEnd;
import com.tangrun.mschat.enums.RoomType;
import com.tangrun.mschat.ui.UserSelector;
import com.tangrun.mschat.ui.CallWindowService;
import com.tangrun.mschat.ui.CallRoomActivity;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.mediasoup.droid.lib.RoomStore;
import org.mediasoup.droid.lib.utils.ArchTaskExecutor;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.model.WrapperCommon;
import org.mediasoup.droid.lib.enums.*;
import org.mediasoup.droid.lib.lv.*;
import org.mediasoup.droid.lib.model.Buddy;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/17 10:51
 */
public class UIRoomStore {

    private static final String TAG = "MS_UIRoomStore";

    private static UIRoomStore uiRoomStore;

    public static UIRoomStore getCurrent() {
        return uiRoomStore;
    }

    private String notificationChannelId = null;
    private final int notificationId = 1;
    private final String notificationTag = "UIRoomStore";
    private static final String NOTIFICATION_CHANNEL_ID = "MSCall";
    private static final String NOTIFICATION_CHANNEL_NAME = "音视频通话";

    private Context context;
    private final AudioManager audioManager;
    private Vibrator vibrator;
    private final NotificationManagerCompat notificationManagerCompat;
    private AppCompatActivity activity;

    private final RoomStore roomStore;
    private final RoomClient roomClient;
    private final RoomOptions roomOptions;

    private UICallback uiCallback;
    /**
     * action
     */
    public DefaultButtonAction Action_MicrophoneDisabled;
    public DefaultButtonAction Action_CameraDisabled;
    public DefaultButtonAction Action_CameraNotFacing;
    public DefaultButtonAction Action_SpeakerOn;
    public DefaultButtonAction Action_HangupAction;
    public DefaultButtonAction Action_JoinAction;

    /**
     * 本地连接状态 会话状态
     */
    public ChangedMutableLiveData<ConversationState> localConversationState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<LocalConnectState> localConnectionState = new ChangedMutableLiveData<>();
    public MutableLiveData<Pair<LocalConnectState, ConversationState>> localState = new MutableLiveData<>();
    /**
     * 麦克风 摄像头状态
     */
    public ChangedMutableLiveData<MicrophoneState> microphoneState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<CameraState> cameraState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<SpeakerState> speakerState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<CameraFacingState> cameraFacingState = new ChangedMutableLiveData<>();
    /**
     * 通话时间 显示 hh:mm
     */
    public ChangedMutableLiveData<String> callTime = new ChangedMutableLiveData<>();
    /**
     * 开始通话 结束通话
     */
    public ChangedMutableLiveData<Boolean> calling = new ChangedMutableLiveData<>();
    private Date callStartTime, callEndTime;
    /**
     *
     */
    public ChangedMutableLiveData<Boolean> showActivity = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Boolean> showWindow = new ChangedMutableLiveData<>();
    /**
     * 把client的人 转化成model 并监听client发来的变化再设置到model里
     */
    public List<BuddyModel> buddyModels = new ArrayList<>();
    public ChangedMutableLiveData<BuddyModel> mine = new ChangedMutableLiveData<>();
    private Map<String, BuddyModel> buddyModelMap = new ConcurrentHashMap<>();
    /**
     * 人进入退出监听
     */
    public DispatcherObservable<IBuddyModelObserver> buddyObservable = new DispatcherObservable<>(IBuddyModelObserver.class);

    private int connectedCount;
    private int joinedCount;

    /**
     * 0 单人
     * 1 多人
     * 默认0 单人
     */
    public RoomType roomType;
    /**
     * 连接上自动打开扬声器
     */
    public boolean firstSpeakerOn = false;
    /**
     * 邀请通话者
     */
    public boolean owner = false;
    /**
     * 最开始 自动join 一般是房主
     * 默认 false
     */
    public boolean firstConnectedAutoJoin = false;
    /**
     * 首次连接自动创建音频流
     * 默认 false
     */
    public boolean firstJoinedAutoProduceAudio = false;
    /**
     * 首次连接自动创建视频流
     * 默认 false
     */
    public boolean firstJoinedAutoProduceVideo = false;
    /**
     * 这个只在自动创建音视频流时使用
     * 实际的上传 接收流设置
     *
     * @see RoomOptions
     */
    public boolean audioOnly = true;
    /**
     * 通话已结束标记 0没挂断 1挂断 2超时 3客户端断开 4对方忙线
     */
    private int callEndFlag = 0;


    /**
     * 连接状态改变监听
     */
    Observer<LocalConnectState> localConnectStateObserver = connectionState1 -> {
        Log.d(TAG, "ConnectionState changed: " + connectionState1);

        localState.postValue(new Pair<>(localConnectionState.getValue(), localConversationState.getValue()));

        // 设置本地会话状态逻辑
        if (connectionState1 == LocalConnectState.CONNECTED) {
            boolean needJoin = false;

            // 首次连接上 自动join
            if (firstConnectedAutoJoin && connectedCount == 0) {
                needJoin = true;
            }

            // 扬声器
            if (firstSpeakerOn && connectedCount == 0) {
                switchSpeakerphoneEnable(true);
            }

            // 重连时自动join
            if (joinedCount > 0) {
                needJoin = true;
            }

            if (needJoin) {
                getRoomClient().join();
            }

            connectedCount++;
        } else if (connectionState1 == LocalConnectState.JOINED) {
            // 网络中断重连时 join后 重连transport
            // 用重连transport无效 因为socket重连后是新的对象 之前的数据都没了 所以只能根据自己本地的状态判断去在重连上后主动传流
            if (joinedCount > 0) {
                if (cameraState.getValue() == CameraState.enabled) {
                    getRoomClient().enableCam();
                }
                if (microphoneState.getValue() == MicrophoneState.enabled) {
                    getRoomClient().enableMic();
                }
            }

            // 首次join后 自动发送流
            if ((firstJoinedAutoProduceAudio || firstJoinedAutoProduceVideo) && joinedCount == 0) {
                if (activity == null) return;
                if (firstJoinedAutoProduceVideo && cameraState.getValue() == CameraState.disabled)
                    switchCamEnable(activity);
                if (firstJoinedAutoProduceAudio && microphoneState.getValue() == MicrophoneState.disabled)
                    switchMicEnable(activity);
            }

            if (joinedCount == 0 && !owner) {
                localConversationState.applyPost(ConversationState.Joined);
            }

            joinedCount++;
        } else if (connectionState1 == LocalConnectState.CLOSED) {
            if (callEndFlag == 1) {
                if (owner)
                    localConversationState.applyPost(ConversationState.Left);
                else {
                    if (joinedCount > 0) {
                        localConversationState.applyPost(ConversationState.Left);
                    } else {
                        localConversationState.applyPost(ConversationState.InviteReject);
                    }
                }
            } else if (callEndFlag == 2) {
                localConversationState.applyPost(ConversationState.InviteTimeout);
            } else if (callEndFlag == 0) {
                // 网络中断
                callEndFlag = 3;
                localConversationState.applyPost(ConversationState.OfflineTimeout);
                hangup();
            }
        }
    };

    Observer<ConversationState> localConversationObserver = conversationState -> {
        localState.postValue(new Pair<>(localConnectionState.getValue(), localConversationState.getValue()));

        // 铃声
        if (conversationState == ConversationState.New) {
            startPlayer(context, R.raw.ms_ring, true);
        } else if (conversationState == ConversationState.Invited) {
            startPlayer(context, R.raw.ms_inviting, true);
        } else if (conversationState == ConversationState.Joined) {
            // 开始通话光靠会话状态不行 还要开始通话标记判断 在开始通话计时时调用一次
            // 最新修改 房主改为开始通话才触发状态到joined
            stopPlayer();
            vibrator.vibrate(50);
        } else if (conversationState == ConversationState.InviteBusy) {
            startPlayer(context, R.raw.ms_busy, false);
        } else {
            startPlayer(context, R.raw.ms_tone, false);
        }
    };

    /**
     * 声音变化设置 因为服务器只会返回正在说话的人列表 所以采用延时设置不说话的方式实现监听每一个人是否在说话
     */
    Observable<Long> volumeDelaySilentObservable = Observable.timer(1200, TimeUnit.MILLISECONDS);
    Map<String, DisposableObserver<Long>> volumeDelaySilentObserverMap = new ConcurrentHashMap<>();

    /**
     * client监听 其他设置都是设置model的属性 livedata用post buddy监听是接口分发要用主线程
     */
    ClientObserver clientObserver = new ClientObserver() {
        @Override
        public void onBuddyAdd(String id, Buddy buddy) {
            ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    BuddyModel buddyModel = new BuddyModel(buddy);
                    buddyModel.connectionState.applyPost(buddy.getConnectionState());
                    buddyModel.conversationState.applyPost(buddy.getConversationState());

                    buddyModels.add(buddyModel);
                    int pos = buddyModels.indexOf(buddyModel);
                    buddyModelMap.put(id, buddyModel);

                    buddyObservable.getDispatcher().onBuddyAdd(pos, buddyModel);

                    // 把自己存起来
                    if (mine.getValue() == null && buddy.isProducer()) {
                        mine.applyPost(buddyModel);
                    }
                }
            });
        }

        @Override
        public void onBuddyRemove(String id) {
            ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    BuddyModel buddyModel = buddyModelMap.get(id);
                    if (buddyModel == null) return;
                    int pos = buddyModels.indexOf(buddyModel);
                    buddyModels.remove(buddyModel);
                    buddyModelMap.remove(id);

                    buddyObservable.getDispatcher().onBuddyRemove(pos, buddyModel);

                    // 去掉了开始通话时间为空判断 因为有可能没有开始通话 即无人接听的情况
                    // 由于现在服务器是先发送状态改变 再发送离开 所以会有延时 用状态改变判断最佳
//                    if (getRoomStore().getBuddys().size() == 1) {
//                        hangup();
//                    }
                }
            });
        }

        @Override
        public void onBuddyVolumeChanged(String id, Buddy buddy) {
            ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    BuddyModel buddyModel = buddyModelMap.get(id);
                    if (buddyModel == null) return;
                    buddyModel.volume.applyPost(buddy.getVolume());

                    // 延迟设置音量为0
                    DisposableObserver<Long> disposableObserver = volumeDelaySilentObserverMap.get(id);
                    if (disposableObserver != null) disposableObserver.dispose();
                    disposableObserver = new DisposableObserver<Long>() {
                        @Override
                        public void onNext(@NotNull Long aLong) {
                            buddyModel.volume.applyPost(0);
                        }

                        @Override
                        public void onError(@NotNull Throwable e) {

                        }

                        @Override
                        public void onComplete() {
                            dispose();
                            volumeDelaySilentObserverMap.remove(id);
                        }
                    };
                    volumeDelaySilentObserverMap.put(id, disposableObserver);
                    volumeDelaySilentObservable.subscribe(disposableObserver);
                }
            });
        }

        @Override
        public void onBuddyStateChanged(String id, Buddy buddy) {
            ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    BuddyModel buddyModel = buddyModelMap.get(id);
                    if (buddyModel == null) return;
                    buddyModel.connectionState.applyPost(buddy.getConnectionState());
                    buddyModel.conversationState.applyPost(buddy.getConversationState());

                    // 第一个人进来就算开始通话
                    if (owner && !buddy.isProducer() && callStartTime == null && joinedCount > 0
                            && buddy.getConnectionState() == ConnectionState.Online && buddy.getConversationState() == ConversationState.Joined) {
                        calling.applyPost(true);
                    }

                    // 自己在接听界面但是长时间没接
                    if (buddy.isProducer() && buddy.getConversationState() == ConversationState.InviteTimeout) {
                        callEndFlag = 2;
                        hangup();
                    }

                    // 除了自己最后一个人变成不活跃状态时 挂断
                    if (!buddy.isProducer() && !isActiveBuddy(buddy)) {
                        boolean hasActiveBuddy = false;
                        for (BuddyModel model : buddyModels) {
                            if (!model.buddy.isProducer() && isActiveBuddy(model.buddy)) {
                                hasActiveBuddy = true;
                                break;
                            }
                        }
                        if (!hasActiveBuddy) {
                            if (roomType == RoomType.SingleCall) {
                                if (buddyModel.conversationState.getValue() == ConversationState.InviteBusy) {
                                    callEndFlag = 4;
                                } else if (buddyModel.conversationState.getValue() == ConversationState.InviteTimeout) {
                                    callEndFlag = 2;
                                }
                            }
                            hangup();
                        }
                    }
                }
            });
        }


        @Override
        public void onProducerAdd(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon) {
            BuddyModel buddyModel = buddyModelMap.get(id);
            if (buddyModel == null) return;
            if (Kind.audio.value.equals(wrapperCommon.getKind())) {
                buddyModel.audioWrapper.applyPost(wrapperCommon);
                buddyModel.audioTrack.applyPost(wrapperCommon.getTrack());
                buddyModel.disabledMic.applyPost(false);
            } else {
                buddyModel.videoWrapper.applyPost(wrapperCommon);
                buddyModel.videoTrack.applyPost(wrapperCommon.getTrack());
                buddyModel.disabledCam.applyPost(false);
            }
        }

        @Override
        public void onProducerRemove(String id, Buddy buddy, String producerId) {
            BuddyModel buddyModel = buddyModelMap.get(id);
            if (buddyModel == null) return;
            if (buddyModel.audioWrapper.getValue() != null && buddyModel.audioWrapper.getValue().getId().equals(producerId)) {
                buddyModel.audioWrapper.applyPost(null);
                buddyModel.audioTrack.applyPost(null);
                buddyModel.disabledMic.applyPost(true);
                buddyModel.volume.applyPost(null);
                return;
            }
            if (buddyModel.videoWrapper.getValue() != null && buddyModel.videoWrapper.getValue().getId().equals(producerId)) {
                buddyModel.videoWrapper.applyPost(null);
                buddyModel.videoTrack.applyPost(null);
                buddyModel.disabledCam.applyPost(true);
            }
        }

        @Override
        public void onProducerResumed(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon) {
            BuddyModel buddyModel = buddyModelMap.get(id);
            if (buddyModel == null) return;
            if (Kind.audio.value.equals(wrapperCommon.getKind())) {
                buddyModel.audioPaused.applyPost(false);
            } else {
                buddyModel.videoPaused.applyPost(false);
            }
        }

        @Override
        public void onProducerPaused(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon) {
            BuddyModel buddyModel = buddyModelMap.get(id);
            if (buddyModel == null) return;
            if (Kind.audio.value.equals(wrapperCommon.getKind())) {
                buddyModel.audioPaused.applyPost(true);
            } else {
                buddyModel.videoPaused.applyPost(true);
            }
        }

        @Override
        public void onProducerScoreChanged(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon) {
            BuddyModel buddyModel = buddyModelMap.get(id);
            if (buddyModel == null) return;
            if (Kind.audio.value.equals(wrapperCommon.getKind())) {
                buddyModel.audioPScore.applyPost(wrapperCommon.getProducerScore());
                buddyModel.audioCScore.applyPost(wrapperCommon.getConsumerScore());
            } else {
                buddyModel.videoPScore.applyPost(wrapperCommon.getProducerScore());
                buddyModel.videoCScore.applyPost(wrapperCommon.getConsumerScore());
            }
        }

        @Override
        public void onLocalConnectStateChanged(LocalConnectState state) {
            localConnectionState.applyPost(state);

            // 被邀请时 自己接听就算开始通话
            if (callStartTime == null && !owner && state == LocalConnectState.JOINED && joinedCount == 0) {
                calling.applyPost(true);
            }
        }

        @Override
        public void onCameraStateChanged(CameraState state) {
            cameraState.applyPost(state);

            // 关闭后再打开还是默认前摄 所以重置一下
            if (state == CameraState.disabled) {
                cameraFacingState.applyPost(CameraFacingState.front);
            }
        }

        @Override
        public void onMicrophoneStateChanged(MicrophoneState state) {
            microphoneState.applyPost(state);
        }

        @Override
        public void onCameraFacingChanged(CameraFacingState state) {
            cameraFacingState.applyPost(state);
        }


    };

    /**
     * 前后台监听 主要用户窗口的打开关闭
     */
    LifecycleEventObserver appProcessObserver = new LifecycleEventObserver() {
        @Override
        public void onStateChanged(@androidx.annotation.NonNull @NotNull LifecycleOwner source, @androidx.annotation.NonNull @NotNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                source.getLifecycle().removeObserver(this);
            } else if (event == Lifecycle.Event.ON_RESUME) {
                // 前台时刷新一下通知 防止进入时清空消息通知把通话通知也清掉了
                updateNotification();
            } else if (event == Lifecycle.Event.ON_STOP) {
                showWindow.applyPost(true);
                openWindowService();
            }
        }
    };

    LifecycleEventObserver activityObserver = new LifecycleEventObserver() {
        @Override
        public void onStateChanged(@androidx.annotation.NonNull @NotNull LifecycleOwner source, @androidx.annotation.NonNull @NotNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                source.getLifecycle().removeObserver(this);
                activity = null;
            } else if (event == Lifecycle.Event.ON_RESUME) {
                showWindow.applyPost(false);
            }
        }
    };


    public UIRoomStore(Context context, RoomOptions roomOptions, RoomType roomType, boolean owner, boolean audioOnly, UICallback uiCallback) {
        this.context = context;
        this.roomClient = new RoomClient(context, roomOptions);
        this.roomStore = roomClient.getStore();
        this.roomOptions = roomClient.getOptions();
        this.roomType = roomType;
        this.owner = owner;
        this.audioOnly = audioOnly;
        this.uiCallback = uiCallback;
        audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        notificationManagerCompat = NotificationManagerCompat.from(context);
        init();
    }

    public void bindLifeOwner(AppCompatActivity owner) {
        if (activity != null) return;
        activity = owner;
        showActivity.applySet(true);
        activity.getLifecycle().addObserver(activityObserver);
        // 因为action涉及到
        cameraState.observe(activity, state -> Action_CameraDisabled.setChecked(state == CameraState.disabled));
        microphoneState.observe(activity, state -> Action_MicrophoneDisabled.setChecked(state == MicrophoneState.disabled));
        cameraFacingState.observe(activity, state -> Action_CameraNotFacing.setChecked(state == CameraFacingState.rear));
        speakerState.observe(activity, state -> Action_SpeakerOn.setChecked(state == SpeakerState.on));
    }

    private void init() {
        {
            localConversationState.applyPost(owner ? ConversationState.New : ConversationState.Invited);
            localConnectionState.applyPost(getRoomStore().getLocalConnectionState());
            microphoneState.applyPost(getRoomStore().getMicrophoneState());
            cameraFacingState.applyPost(getRoomStore().getCameraFacingState());
            cameraState.applyPost(getRoomStore().getCameraState());
            for (Buddy value : getRoomStore().getBuddys().values()) {
                clientObserver.onBuddyAdd(value.getId(), value);
            }
        }

        // action 操作 初始化
        {
            Action_JoinAction = new DefaultButtonAction("", audioOnly ? R.drawable.ms_selector_call_answer_audio : R.drawable.ms_selector_call_answer_video) {
                @Override
                public void onClick(View v) {
                    join();
                }
            };
            Action_HangupAction = new DefaultButtonAction("", R.drawable.ms_selector_call_hangup) {
                @Override
                public void onClick(View v) {
                    hangup();
                    calling.applySet(false);
                }
            };
            Action_MicrophoneDisabled = new DefaultButtonAction("麦克风", R.drawable.ms_selctor_microphone_disabled) {
                @Override
                public void onClick(View v) {
                    switchMicEnable(v.getContext());
                }
            };
            Action_SpeakerOn = new DefaultButtonAction("免提", R.drawable.ms_selector_speaker_on) {
                @Override
                public void onClick(View v) {
                    switchSpeakerphoneEnable();
                }
            };
            Action_CameraDisabled = new DefaultButtonAction("摄像头", R.drawable.ms_selector_camera_disabled) {
                @Override
                public void onClick(View v) {
                    switchCamEnable(v.getContext());
                }
            };
            Action_CameraNotFacing = new DefaultButtonAction("切换摄像头", R.drawable.ms_selector_camera_rear) {
                @Override
                public void onClick(View v) {
                    switchCamDevice();
                }
            };
        }
        // 通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setLightColor(0);
            channel.setSound(null, null);
            channel.setVibrationPattern(new long[]{});
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannelId = NOTIFICATION_CHANNEL_ID;
            NotificationManagerCompat.from(context).createNotificationChannel(channel);
        }
        localState.observeForever(localConnectStateConversationStatePair -> {
            updateNotification();
        });
        // 设置本地会话状态 本地连接状态由client维护
        localConnectionState.observeForever(localConnectStateObserver);
        localConversationState.observeForever(localConversationObserver);
        // 前后台切换监听
        ProcessLifecycleOwner.get().getLifecycle().addObserver(appProcessObserver);
        // 结束通话
        calling.observeForever(aBoolean -> {
            if (aBoolean) {
                startCallTime();
                if (owner) {
                    localConversationState.applyPost(ConversationState.Joined);
                }
            } else {
                stopCallTime();
                release();
                setCallEnd();
                ArchTaskExecutor.getInstance().postToMainThread(() -> {
                    showActivity.applyPost(false);
                    showWindow.applyPost(false);
                }, 1500);
            }
        });

        getRoomStore().getClientObservable().registerObserver(clientObserver);
    }

    private void setCallEnd() {
        if (uiCallback == null) return;
        CallEnd callEnd = CallEnd.End;
        if (roomType == RoomType.SingleCall) {
            if (callEndFlag == 1) {
                if (callStartTime == null) {
                    if (owner)
                        callEnd = CallEnd.Cancel;
                    else
                        callEnd = CallEnd.Reject;
                } else {
                    callEnd = CallEnd.End;
                }
            } else if (callEndFlag == 2) {
                callEnd = CallEnd.NoAnswer;
            } else if (callEndFlag == 3) {
                callEnd = CallEnd.End;
            } else if (callEndFlag == 4) {
                callEnd = CallEnd.Busy;
            }
        }

        uiCallback.onCallEnd(getRoomOptions().roomId, roomType, audioOnly, callEnd, callStartTime, callEndTime);
    }

    private void updateNotification() {
        ConversationState conversationState = localConversationState.getValue();
        LocalConnectState connectState = localConnectionState.getValue();
        if (conversationState == ConversationState.New) {
            if (owner) {
                if (connectState == LocalConnectState.JOINED)
                    setNotification("等待对方接听");
                else {
                    setNotification("连接中...");
                }
            } else {
                setNotification("等待对方接听");
            }
        } else if (conversationState == ConversationState.Invited) {
            setNotification("待接听");
        } else if (conversationState == ConversationState.Joined) {
            setNotification("通话中...");
        } else {
            setNotification("通话已结束");
        }
    }

    private void release() {
        cancelNotification();
        localConnectionState.removeObserver(localConnectStateObserver);
        localConversationState.removeObserver(localConversationObserver);
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(appProcessObserver);
        getRoomStore().getClientObservable().unregisterAll();
        stopCallTime();
        stopPlayer();
    }

    private boolean isActiveBuddy(Buddy buddy) {
        if (buddy == null) return false;
        if (buddy.getConversationState() == ConversationState.InviteBusy
                || buddy.getConversationState() == ConversationState.InviteReject
                || buddy.getConversationState() == ConversationState.OfflineTimeout
                || buddy.getConversationState() == ConversationState.InviteTimeout
                || buddy.getConversationState() == ConversationState.Left
        ) {
            return false;
        }
        return true;
    }

    //region 通话组件启动

    private Intent getCallActivityIntent() {
        return new Intent(context, CallRoomActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    private Intent getCallServiceIntent() {
        return new Intent(context, CallWindowService.class);
    }

    public void openCallActivity() {
        context.startActivity(getCallActivityIntent());
    }

    private void openWindowService() {
        context.startService(getCallServiceIntent());
    }

    //endregion

    //region 通话时间计时器

    private DisposableObserver<Long> callTimeObserver;

    private void startCallTime() {
        if (callStartTime != null) return;
        callStartTime = new Date();
        callTimeObserver = new DisposableObserver<Long>() {
            @Override
            public void onNext(@NonNull Long aLong) {
                long time = System.currentTimeMillis() - callStartTime.getTime();
                long minute = time / 1000 / 60;
                long second = time / 1000 - minute * 60;
                callTime.applyPost(String.format("%02d:%02d", minute, second));
            }

            @Override
            public void onError(@NonNull Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        Observable.interval(0, 1, TimeUnit.SECONDS)
                .subscribe(callTimeObserver);
    }

    private void stopCallTime() {
        if (callStartTime == null || callEndTime != null) return;
        if (callTimeObserver != null)
            callTimeObserver.dispose();
        callTimeObserver = null;
        callEndTime = new Date();
    }

    //endregion

    // region action功能操作

    public void connect() {
        getRoomClient().connect();
    }

    public void join() {
        if (localConnectionState.getValue() == LocalConnectState.CONNECTED)
            getRoomClient().join();
        else {
            toast("请稍等，连接中...");
        }
    }

    public void hangup() {
        stopCallTime();
        if (callEndFlag == 0)
            callEndFlag = 1;
        if (localConnectionState.getValue() == LocalConnectState.JOINED
                || localConnectionState.getValue() == LocalConnectState.CONNECTED)
            getRoomClient().hangup();
        else {
            getRoomClient().close();
        }
        ArchTaskExecutor.getInstance().postToMainThread(new Runnable() {
            @Override
            public void run() {
                calling.applySet(false);
            }
        }, 1500);
    }

    public void switchMicEnable(Context context) {
        if (!XXPermissions.isGranted(context, Permission.RECORD_AUDIO)) {
            showDialog(context, "权限请求", "语音通话需要录音权限，请授予",
                    "取消", null,
                    "好的", new Runnable() {
                        @Override
                        public void run() {
                            XXPermissions.with(context).permission(Permission.RECORD_AUDIO).request((PermissionCallback) (all, never) -> {
                                if (all) {
                                    switchMicEnable(context);
                                } else {
                                    if (never) {
                                        showDialog(context, "权限请求", "麦克风权限已经被永久拒绝了，再次开启需要前往应用权限页面手动开启",
                                                "取消", null,
                                                "打开权限页面", () -> {
                                                    XXPermissions.startPermissionActivity(context, Permission.RECORD_AUDIO);
                                                });
                                    } else {
                                        showDialog(context, "权限请求", "没有麦克风权限将无法进行正常的语音通话，是否重新授予？",
                                                "取消", null,
                                                "是的", this);
                                    }
                                }
                            });
                        }
                    });
            return;
        }
        if (microphoneState.getValue() == MicrophoneState.disabled)
            getRoomClient().enableMic();
        else if (microphoneState.getValue() == MicrophoneState.enabled)
            getRoomClient().disableMic();
    }

    public void switchCamEnable(Context context) {
        if (!XXPermissions.isGranted(context, Permission.CAMERA)) {
            showDialog(context, "权限请求", "视频通话需要摄像头权限，请授予", "取消", null, "好的", new Runnable() {
                @Override
                public void run() {
                    XXPermissions.with(context).permission(Permission.CAMERA).request((PermissionCallback) (all, never) -> {
                        if (all) {
                            switchCamEnable(context);
                        } else {
                            if (never) {
                                showDialog(context, "权限请求", "摄像头权限已经被永久拒绝了，再次开启需要前往应用权限页面手动开启",
                                        "取消", null,
                                        "打开权限页面", () -> {
                                            XXPermissions.startPermissionActivity(context, Permission.CAMERA);
                                        });
                            } else {
                                showDialog(context, "权限请求", "没有摄像头权限将无法进行正常的视频通话，是否重新授予？",
                                        "取消", null,
                                        "是的", this);
                            }
                        }
                    });
                }
            });
            return;
        }
        if (cameraState.getValue() == CameraState.disabled)
            getRoomClient().enableCam();
        else if (cameraState.getValue() == CameraState.enabled)
            getRoomClient().disableCam();
    }

    private void setSpeakerphoneOn(boolean isSpeakerphoneOn) {
        audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
        audioManager.setMode(isSpeakerphoneOn ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
    }

    private void switchSpeakerphoneEnable(boolean enable) {
        setSpeakerphoneOn(enable);
        speakerState.setValue(enable ? SpeakerState.on : SpeakerState.off);
    }

    public void switchSpeakerphoneEnable() {
        if (speakerState.getValue() == SpeakerState.on) {
            switchSpeakerphoneEnable(false);
        } else if (speakerState.getValue() == SpeakerState.off) {
            switchSpeakerphoneEnable(true);
        }
    }

    public void switchCamDevice() {
        if (cameraState.getValue() != CameraState.enabled) return;
        getRoomClient().changeCam();
    }

    public void onMinimize(Activity activity) {
        if (!XXPermissions.isGranted(activity, Permission.SYSTEM_ALERT_WINDOW)) {
            showDialog(activity, "权限请求", "窗口显示需要开启悬浮窗权限，请授予",
                    "取消", null,
                    "好的", new Runnable() {
                        @Override
                        public void run() {
                            XXPermissions.with(activity).permission(Permission.SYSTEM_ALERT_WINDOW).request((PermissionCallback) (all, never) -> {
                                if (all) {
                                    onMinimize(activity);
                                } else {
                                    if (never) {
                                        showDialog(activity, "权限请求", "悬浮窗权限已经被永久拒绝了，再次开启需要前往应用权限页面手动开启",
                                                "取消", null,
                                                "打开权限页面", () -> {
                                                    XXPermissions.startPermissionActivity(activity, Permission.SYSTEM_ALERT_WINDOW);
                                                });
                                    } else {
                                        showDialog(activity, "权限请求", "没有悬浮窗权限将无法进行窗口显示，是否重新授予？",
                                                "取消", null,
                                                "是的", this);
                                    }
                                }
                            });
                        }
                    },
                    "关闭页面", activity::finish);
            return;
        }
        showActivity.applyPost(false);
        showWindow.applyPost(true);
        openWindowService();
    }

    public void onAddUserClick(Context context) {
        if (uiCallback == null) return;
        List<User> list = new ArrayList<>();
        for (Buddy buddy : getRoomStore().getBuddys().values()) {
            User user = new User();
            user.setId(buddy.getId());
            user.setAvatar(buddy.getAvatar());
            user.setDisplayName(buddy.getDisplayName());
            list.add(user);
        }
        Intent intent = uiCallback.getAddUserIntent(context, list);
        if (intent == null) return;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        UserSelector.start(context, intent);
    }

    public void onAddUserResult(int resultCode, Intent data) {
        if (uiCallback != null) uiCallback.onAddUserResult(resultCode, data);
    }

    public void addUser(List<User> list) {
        if (list == null || list.isEmpty()) return;
        localConnectionState.observeForever(new Observer<LocalConnectState>() {
            @Override
            public void onChanged(LocalConnectState state) {
                if (
                        state == LocalConnectState.CONNECTED
                                || state == LocalConnectState.JOINED
                ) {
                    localConnectionState.removeObserver(this);
                    JSONArray jsonArray = new JSONArray();
                    for (User user : list) {
                        jsonArray.put(user.toJsonObj());
                    }
                    getRoomClient().addPeers(jsonArray);
                }
            }
        });
    }


    // endregion

    //region 内部使用封装方法 音乐播放 通知栏什么的

    private MediaPlayer player;

    private void stopPlayer() {
        Log.d(TAG, "stopRing() called");
        if (player != null) {
            try {
                player.stop();
            } catch (Exception e) {
                e.printStackTrace();
                player = null;
                player = new MediaPlayer();
                player.stop();
            }
            player.release();
            player = null;
        }
    }

    private void startPlayer(Context context, int id, boolean loop) {
        Log.d(TAG, "startRingForType() called with: id = [" + id + "], loop = [" + loop + "]");
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + id);
        if (player == null) {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_RING);
        } else {
            try {
                player.stop();
            } catch (Exception e) {
                e.printStackTrace();
                player = null;
                player = new MediaPlayer();
                player.setAudioStreamType(AudioManager.STREAM_RING);
                player.stop();
            }
        }
        if (uri == null) return;
        try {
            player.reset();
            player.setDataSource(context, uri);
            player.setLooping(loop);
            player.prepare();
            player.start();
            if (!loop)
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopPlayer();
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cancelNotification() {
        notificationManagerCompat.cancel(notificationTag, notificationId);
    }

    private void setNotification(String content) {
        Notification notification = new NotificationCompat.Builder(context, notificationChannelId)
                .setOngoing(true)
                .setSmallIcon(context.getApplicationInfo().icon)
                .setContentIntent(PendingIntent.getActivity(context, 0, getCallActivityIntent(), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentText(content)
                .build();

        notificationManagerCompat
                .notify(notificationTag, notificationId, notification);
    }

    private void showDialog(Context context, String title, String msg,
                            String negativeText, Runnable negative,
                            String positiveText, Runnable positive,
                            String neutralText, Runnable neutral) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context).setTitle(title).setMessage(msg);
        if (negativeText != null) builder.setNegativeButton(negativeText, (dialog, which) -> {
            dialog.dismiss();
            if (negative != null) negative.run();
        });
        if (positiveText != null) builder.setPositiveButton(positiveText, (dialog, which) -> {
            dialog.dismiss();
            if (positive != null) positive.run();
        });
        if (neutralText != null) builder.setNeutralButton(neutralText, (dialog, which) -> {
            dialog.dismiss();
            if (neutral != null) neutral.run();
        });
        builder.setCancelable(false).show();
    }

    private void showDialog(Context context, String title, String msg,
                            String negativeText, Runnable negative,
                            String positiveText, Runnable positive) {
        showDialog(context, title, msg, negativeText, negative, positiveText, positive, null, null);
    }

    private void toast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }


    //endregion


    public RoomStore getRoomStore() {
        return roomStore;
    }

    public RoomClient getRoomClient() {
        return roomClient;
    }

    public RoomOptions getRoomOptions() {
        return roomOptions;
    }

    private interface PermissionCallback extends OnPermissionCallback {
        void onResult(boolean all, boolean never);

        @Override
        default void onGranted(List<String> permissions, boolean all) {
            if (all) onResult(true, false);
        }

        @Override
        default void onDenied(List<String> permissions, boolean never) {
            onResult(false, never);
        }
    }

    public static abstract class ButtonAction<V> implements View.OnClickListener {
        protected String name;
        protected int imgId;
        protected boolean checked;
        protected V v;

        public abstract void bindView(V v);

        public abstract void setChecked(boolean checked);


    }

    public abstract static class DefaultButtonAction extends ButtonAction<MsLayoutActionBinding> {


        public DefaultButtonAction(String name, int imgId) {
            this.name = name;
            this.imgId = imgId;
        }

        @Override
        public void bindView(MsLayoutActionBinding itemActionBinding) {
            this.v = itemActionBinding;
            if (itemActionBinding == null) return;
            itemActionBinding.llContent.setVisibility(View.VISIBLE);
            itemActionBinding.ivImg.setOnClickListener(this);
            itemActionBinding.tvContent.setText(name);
            itemActionBinding.ivImg.setImageResource(imgId);
            itemActionBinding.ivImg.setSelected(checked);
        }


        @Override
        public void setChecked(boolean checked) {
            this.checked = checked;
            if (v != null) {
                v.ivImg.setSelected(checked);
            }
        }

    }


}
