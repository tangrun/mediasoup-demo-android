package com.tangrun.mschat.model;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.tangrun.mschat.R;
import com.tangrun.mschat.UICallback;
import com.tangrun.mschat.databinding.MsLayoutActionBinding;
import com.tangrun.mschat.enums.CallEnd;
import com.tangrun.mschat.enums.RoomType;
import com.tangrun.mschat.ui.CallNotificationService;
import com.tangrun.mschat.ui.CallRoomActivity;
import com.tangrun.mschat.ui.CallWindowService;
import com.tangrun.mschat.ui.UserSelector;
import com.tangrun.mschat.view.InitSurfaceViewRender;
import com.tangrun.mslib.RoomClient;
import com.tangrun.mslib.RoomOptions;
import com.tangrun.mslib.RoomStore;
import com.tangrun.mslib.enums.*;
import com.tangrun.mslib.lv.ChangedMutableLiveData;
import com.tangrun.mslib.lv.ClientObserver;
import com.tangrun.mslib.lv.DispatcherObservable;
import com.tangrun.mslib.model.Buddy;
import com.tangrun.mslib.model.WrapperCommon;
import com.tangrun.mslib.utils.ArchTaskExecutor;
import com.tangrun.mslib.utils.PeerConnectionUtils;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/17 10:51
 */
public class UIRoomStore implements Parcelable {

    private static final String TAG = "MS_UIRoomStore";

    private static WeakReference<UIRoomStore> uiRoomStore = new WeakReference<>(null);

    public static UIRoomStore getCurrent() {
        return uiRoomStore == null ? null : uiRoomStore.get();
    }


    private Context context;
    private  AudioManager audioManager;
    private Vibrator vibrator;

    private ChangedMutableLiveData<AppCompatActivity> activity = new ChangedMutableLiveData<>();

    private  RoomStore roomStore;
    private  RoomClient roomClient;
    private  RoomOptions roomOptions;

    public UICallback uiCallback;
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
    public TransportState sendTransportState = TransportState.none;
    public TransportState recvTransportState = TransportState.none;
    /**
     * 通话时间 显示 hh:mm
     */
    public ChangedMutableLiveData<String> callTime = new ChangedMutableLiveData<>();
    /**
     * 开始通话 结束通话 结束有延迟 用于关闭页面等判断
     */
    public ChangedMutableLiveData<Boolean> calling = new ChangedMutableLiveData<>();
    /**
     * 实时的 如点击挂断之后就立马有效了
     * 新增：因为挂断之后transport断开 会发送track为null 但是render里旧的对象还在 底层已经释放了 就会出问题 所以重新通过这个简单判断一下
     */
    public ChangedMutableLiveData<Boolean> callingActual = new ChangedMutableLiveData<>();
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
    private final Map<String, BuddyModel> buddyModelMap = new ConcurrentHashMap<>();
    /**
     * 人进入退出监听
     */
    public DispatcherObservable<IBuddyModelObserver> buddyObservable = new DispatcherObservable<>(IBuddyModelObserver.class);

    private int connectedCount;
    private int joinedCount;
    public int activityBindCount;
    /**
     * 首次连接传的用户 保存一下做判断用
     */
    private List<User> firstConnectUsers;

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
    public boolean owner;
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
    public boolean audioOnly;
    /**
     * 创建时间
     */
    public Date createTime = new Date();
    /**
     * 通话已结束标记 0没挂断 1挂断了 并且设置了callend了  2客户端断开（网络）
     */
    private int callEndFlag = 0;
    private CallEnd callEnd = CallEnd.None;


    /**
     * 连接状态改变监听
     */
    Observer<LocalConnectState> localConnectStateObserver = connectionState1 -> {
        Log.d(TAG, "ConnectionState changed: " + connectionState1);
        localState.setValue(new Pair<>(connectionState1, localConversationState.getValue()));

        // 设置本地会话状态逻辑
        if (connectionState1 == LocalConnectState.CONNECTED) {

            //自动join
            if ((firstConnectedAutoJoin && connectedCount == 0) // 首次连接上
                    || (joinedCount > 0) // 重连时自动join
            ) {
                getRoomClient().join();
            }

            // 单聊上线 对方已经离开检测
            if (roomType == RoomType.SingleCall && connectedCount == 0 && firstConnectUsers != null && !firstConnectUsers.isEmpty()) {
                String targetId = firstConnectUsers.get(0).getId();
                Observable.timer(1000, TimeUnit.MILLISECONDS)
                        .subscribe(new DisposableObserver<Long>() {
                            @Override
                            public void onNext(@NotNull Long aLong) {
                                if (buddyModelMap.get(targetId) != null) return;
                                getRoomClient().getPeer(targetId, new Consumer<Buddy>() {
                                    @Override
                                    public void accept(Buddy buddy) {
                                        if (callingActual.getValue() != null || buddyModelMap.get(targetId) != null)
                                            return;
                                        clientObserver.onBuddyAdd(buddy.getId(), buddy);
                                        clientObserver.onBuddyStateChanged(buddy.getId(), buddy);
                                    }
                                });
                            }

                            @Override
                            public void onError(@NotNull Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
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
                activity.observeForever(new Observer<AppCompatActivity>() {
                    @Override
                    public void onChanged(AppCompatActivity appCompatActivity) {
                        if (appCompatActivity != null) {
                            activity.removeObserver(this);
                            appCompatActivity.getLifecycle().addObserver(new LifecycleEventObserver() {
                                @Override
                                public void onStateChanged(@androidx.annotation.NonNull @NotNull LifecycleOwner source, @androidx.annotation.NonNull @NotNull Lifecycle.Event event) {
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        if (firstJoinedAutoProduceVideo && cameraState.getValue() == CameraState.disabled)
                                            switchCamEnable(appCompatActivity);
                                        if (firstJoinedAutoProduceAudio && microphoneState.getValue() == MicrophoneState.disabled)
                                            switchMicEnable(appCompatActivity);
                                        source.getLifecycle().removeObserver(this);
                                    }
                                }
                            });
                        }
                    }
                });
            }

            if (joinedCount == 0 && !owner) {
                localConversationState.applySet(ConversationState.Joined);
            }

            joinedCount++;
        } else if (connectionState1 == LocalConnectState.CLOSED) {
            if (callEndFlag == 0) {
                // 网络中断
                callEndFlag = 2;
                hangup();
            }
        }
    };

    Observer<ConversationState> localConversationObserver = conversationState -> {
        Log.d(TAG, "ConversationState changed: " + conversationState);
        localState.setValue(new Pair<>(localConnectionState.getValue(), conversationState));

        // 铃声
        // 由于状态有延时 所以实例化时就直接播放
//        if (conversationState == ConversationState.New) {
//            startPlayer(context, R.raw.ms_ring, true);
//        } else if (conversationState == ConversationState.Invited) {
//            startPlayer(context, R.raw.ms_inviting, true);
//        } else
        if (conversationState == ConversationState.Joined) {
            // 开始通话光靠会话状态不行 还要开始通话标记判断 在开始通话计时时调用一次
            // 最新修改 房主改为开始通话才触发状态到joined
            stopPlayer();
            vibrator.vibrate(50);
        } else if (conversationState == ConversationState.Left
                || conversationState == ConversationState.InviteTimeout
                || conversationState == ConversationState.InviteReject
                || conversationState == ConversationState.OfflineTimeout
        ) {
            if (callEnd == CallEnd.RemoteBusy) {
                startPlayer(context, R.raw.ms_busy, false);
            } else
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
            ArchTaskExecutor.getMainThreadExecutor().execute(() -> {
                Log.d(TAG, "onBuddyAdd: " + id + buddy.getConnectionState() + buddy.getConversationState());

                BuddyModel buddyModel = new BuddyModel(buddy);
                buddyModel.connectionState.applySet(buddy.getConnectionState());
                buddyModel.conversationState.applySet(buddy.getConversationState());

                buddyModels.add(buddyModel);
                int pos = buddyModels.indexOf(buddyModel);
                buddyModelMap.put(id, buddyModel);

                buddyObservable.getDispatcher().onBuddyAdd(pos, buddyModel);

                // 把自己存起来
                if (mine.getValue() == null && buddy.isProducer()) {
                    mine.applySet(buddyModel);
                }
            });
        }

        @Override
        public void onBuddyRemove(String id) {
            ArchTaskExecutor.getMainThreadExecutor().execute(() -> {
                BuddyModel buddyModel = buddyModelMap.get(id);
                if (buddyModel == null) return;
                Log.d(TAG, "onBuddyRemove: " + id);

                int pos = buddyModels.indexOf(buddyModel);
                buddyModels.remove(buddyModel);
                buddyModelMap.remove(id);

                buddyObservable.getDispatcher().onBuddyRemove(pos, buddyModel);

                // 去掉了开始通话时间为空判断 因为有可能没有开始通话 即无人接听的情况
                // 由于现在服务器是先发送状态改变 再发送离开 所以会有延时 用状态改变判断最佳
//                    if (getRoomStore().getBuddys().size() == 1) {
//                        hangup();
//                    }
            });
        }

        @Override
        public void onBuddyVolumeChanged(String id, Buddy buddy) {
            ArchTaskExecutor.getMainThreadExecutor().execute(() -> {
                BuddyModel buddyModel = buddyModelMap.get(id);
                if (buddyModel == null) return;
                Log.d(TAG, "onBuddyVolumeChanged: " + id + " " + buddy.getVolume());

                buddyModel.volume.applySet(buddy.getVolume());

                // 延迟设置音量为0
                DisposableObserver<Long> disposableObserver = volumeDelaySilentObserverMap.get(id);
                if (disposableObserver != null) disposableObserver.dispose();
                disposableObserver = new DisposableObserver<Long>() {
                    @Override
                    public void onNext(@NotNull Long aLong) {
                        buddyModel.volume.applySet(0);
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
            });
        }

        @Override
        public void onBuddyStateChanged(String id, Buddy buddy) {
            ArchTaskExecutor.getMainThreadExecutor().execute(() -> {
                BuddyModel buddyModel = buddyModelMap.get(id);
                if (buddyModel == null) return;
                Log.d(TAG, "onBuddyStateChanged: " + id + buddy.getConnectionState() + " " + buddy.getConversationState());

                // 第一个人进来就算开始通话
                if (owner && !buddy.isProducer() && callStartTime == null && buddy.getConnectionState() == ConnectionState.Online && buddy.getConversationState() == ConversationState.Joined) {
                    Log.d(TAG, "calling.applySet false by 人接听");
                    if (joinedCount == 0) {
                        localConnectionState.observeForever(new Observer<LocalConnectState>() {
                            @Override
                            public void onChanged(LocalConnectState localConnectState) {
                                localConnectionState.removeObserver(this);
                                if (callingActual.getValue() == null) {
                                    callingActual.applySet(true);
                                    calling.applySet(true);
                                }
                            }
                        });
                    } else {
                        callingActual.applySet(true);
                        calling.applySet(true);
                    }
                }

                // 自己在接听界面但是长时间没接
                if (buddy.isProducer() && buddy.getConversationState() == ConversationState.InviteTimeout) {
                    localConversationState.applySet(ConversationState.InviteTimeout);
                    callEnd = CallEnd.NoAnswer;
                    callEndFlag = 1;
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
                        if (buddy.getConversationState() == ConversationState.InviteBusy) {
                            callEnd = CallEnd.RemoteBusy;
                            callEndFlag = 1;
                        } else if (buddy.getConversationState() == ConversationState.InviteTimeout) {
                            callEnd = CallEnd.RemoteNoAnswer;
                            callEndFlag = 1;
                        } else if (buddy.getConversationState() == ConversationState.InviteReject) {
                            callEnd = CallEnd.RemoteReject;
                            callEndFlag = 1;
                        } else if (buddy.getConversationState() == ConversationState.Left) {
                            // 发起者取消了
                            if (!owner && callingActual.getValue() == null) {
                                callEnd = CallEnd.RemoteCancel;
                                callEndFlag = 1;
                            }
                        }
                        if (callEndFlag == 1) {
                            localConversationState.applySet(ConversationState.Left);
                        }
                        hangup();
                    }
                }

                buddyModel.connectionState.applySet(buddy.getConnectionState());
                buddyModel.conversationState.applySet(buddy.getConversationState());
            });
        }


        @Override
        public void onProducerAdd(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon) {
            BuddyModel buddyModel = buddyModelMap.get(id);
            if (buddyModel == null) return;
            Log.d(TAG, "onProducerAdd: " + id + " " + producerId + " " + wrapperCommon.getKind());

            if (Kind.audio.value.equals(wrapperCommon.getKind())) {
                buddyModel.audioWrapper.applyPost(wrapperCommon);
                buddyModel.audioTrack.applyPost(wrapperCommon.getTrack());
                buddyModel.disabledMic.applyPost(false);
            } else if (Kind.video.value.equals(wrapperCommon.getKind())) {
                buddyModel.videoWrapper.applyPost(wrapperCommon);
                buddyModel.videoTrack.applyPost(wrapperCommon.getTrack());
                buddyModel.disabledCam.applyPost(false);
            }
        }

        @Override
        public void onProducerRemove(String id, Buddy buddy, String producerId) {
            BuddyModel buddyModel = buddyModelMap.get(id);
            if (buddyModel == null) return;
            Log.d(TAG, "onProducerRemove: " + id + " " + producerId);

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
            Log.d(TAG, "onProducerResumed: " + id + " " + producerId);

            if (Kind.audio.value.equals(wrapperCommon.getKind())) {
                buddyModel.audioPaused.applyPost(false);
                buddyModel.disabledMic.applyPost(false);
            } else if (Kind.video.value.equals(wrapperCommon.getKind())) {
                buddyModel.videoPaused.applyPost(false);
                buddyModel.disabledCam.applyPost(false);
            }
        }

        @Override
        public void onProducerPaused(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon) {
            BuddyModel buddyModel = buddyModelMap.get(id);
            if (buddyModel == null) return;
            Log.d(TAG, "onProducerPaused: " + id + " " + producerId);

            if (Kind.audio.value.equals(wrapperCommon.getKind())) {
                buddyModel.audioPaused.applyPost(true);
                buddyModel.disabledMic.applyPost(true);
            } else {
                buddyModel.videoPaused.applyPost(true);
                buddyModel.disabledCam.applyPost(true);
            }
        }

        @Override
        public void onProducerScoreChanged(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon) {
            BuddyModel buddyModel = buddyModelMap.get(id);
            if (buddyModel == null) return;
            Log.d(TAG, "onProducerScoreChanged: " + id + " " + producerId);

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
            Log.d(TAG, "onLocalConnectStateChanged: " + state);

            // 被邀请时 自己接听就算开始通话
            if (callStartTime == null && !owner && state == LocalConnectState.JOINED && joinedCount == 0) {
                Log.d(TAG, "calling.applySet true by 房主");
                callingActual.applyPost(true);
                calling.applyPost(true);
            }

            localConnectionState.applyPost(state);
        }

        @Override
        public void onCameraStateChanged(CameraState state) {
            Log.d(TAG, "onCameraStateChanged: " + state);

            cameraState.applyPost(state);

            // 关闭后再打开还是默认前摄 所以重置一下
            if (state == CameraState.disabled) {
                cameraFacingState.applyPost(CameraFacingState.front);
            }
        }

        @Override
        public void onMicrophoneStateChanged(MicrophoneState state) {
            Log.d(TAG, "onMicrophoneStateChanged: " + state);

            microphoneState.applyPost(state);
        }

        @Override
        public void onCameraFacingChanged(CameraFacingState state) {
            Log.d(TAG, "onCameraFacingChanged: " + state);

            cameraFacingState.applyPost(state);
        }

        @Override
        public void onTransportStateChanged(boolean sender, TransportState state) {
            // 由于这个时间要求比较高 不能采用livedata的方式
            if (sender) {
                sendTransportState = state;
                if (state == TransportState.disposed)
                    releaseProducerBuddyRender();
            } else {
                recvTransportState = state;
                if (state == TransportState.disposed)
                    releaseConsumerBuddyRender();
            }
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
                if (activityBindCount == 0) {
                    openCallActivity();
                }
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
                activity.applySet(null);
            } else if (event == Lifecycle.Event.ON_RESUME) {
                showWindow.applyPost(false);
            }
        }
    };

    public UIRoomStore() {

    }

    public void bindActivity(AppCompatActivity owner) {
        if (activity.getValue() != null) return;
        activityBindCount++;
        activity.applySet(owner);
        showActivity.applySet(true);
        owner.getLifecycle().addObserver(activityObserver);
        // 因为action涉及到
        cameraState.observe(owner, state -> Action_CameraDisabled.setChecked(state == CameraState.disabled));
        microphoneState.observe(owner, state -> Action_MicrophoneDisabled.setChecked(state == MicrophoneState.disabled));
        cameraFacingState.observe(owner, state -> Action_CameraNotFacing.setChecked(state == CameraFacingState.rear));
        speakerState.observe(owner, state -> Action_SpeakerOn.setChecked(state == SpeakerState.on));
        // 距离感应器
        if (audioOnly) {
            setDistanceSensor(owner);
        }
    }

    public void init(Context context, RoomOptions roomOptions) {
        this.context = context;
        this.roomClient = new RoomClient(context, roomOptions);
        this.roomStore = roomClient.getStore();
        this.roomOptions = roomClient.getOptions();
        audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        uiRoomStore = new WeakReference<>(this);
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
            Action_JoinAction = new DefaultButtonAction("接听", audioOnly ? R.drawable.ms_selector_call_answer_audio : R.drawable.ms_selector_call_answer_video) {
                @Override
                public void onClick(View v) {
                    join();
                }
            };
            Action_HangupAction = new DefaultButtonAction("取消", R.drawable.ms_selector_call_hangup) {
                @Override
                public void onClick(View v) {
                    hangup();
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
        // 设置本地会话状态 本地连接状态由client维护
        localConnectionState.observeForever(localConnectStateObserver);
        localConversationState.observeForever(localConversationObserver);
        // 前后台切换监听
        ProcessLifecycleOwner.get().getLifecycle().addObserver(appProcessObserver);
        // 结束通话
        calling.observeForever(aBoolean -> {
            if (aBoolean) {
                startCallTime();
                // 本地会话状态 房主一直是new 直到有人进入开始通话才更改为joined 实际上连接socket状态可能已经joined
                if (owner) {
                    localConversationState.applySet(ConversationState.Joined);
                }
            } else {
                stopCallTime();
                release();
                setCallEnd();
                ArchTaskExecutor.getInstance().postToMainThread(() -> {
                    showActivity.applySet(false);
                    showWindow.applySet(false);
                    uiRoomStore = null;
                }, 1500);
            }
        });
        // 扬声器
        if (firstSpeakerOn) {
            switchSpeakerphoneEnable(true);
        }
        if (owner) {
            startPlayer(context, R.raw.ms_ring, true);
        } else {
            startPlayer(context, R.raw.ms_inviting, true);
        }

        startNotificationService();

        getRoomStore().getClientObservable().registerObserver(clientObserver);
    }


    private void setCallEnd() {
        if (uiCallback == null) return;
        CallEnd callEnd = roomType == RoomType.SingleCall ? this.callEnd : CallEnd.End;
        if (callEnd == CallEnd.None) {
            callEnd = CallEnd.End;
        }

        uiCallback.onCallEnd(getRoomOptions().roomId, roomType, audioOnly, callEnd, callStartTime, callEndTime);
    }


    private void release() {
        localConnectionState.removeObserver(localConnectStateObserver);
        localConversationState.removeObserver(localConversationObserver);
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(appProcessObserver);
        getRoomStore().getClientObservable().unregisterAll();
        stopCallTime();
        stopPlayer();
        setSpeakerphoneOn(false);
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


    //region 视频track和view绑定释放
    Map<BuddyModel, Map<LifecycleOwner, TrackBinder>> buddyModelTrackBinderMap = new ConcurrentHashMap<>();

    private class TrackBinder {
        LifecycleOwner lifecycleOwner;
        BuddyModel buddyModel;
        InitSurfaceViewRender render;

        public TrackBinder(LifecycleOwner lifecycleOwner, BuddyModel buddyModel, InitSurfaceViewRender render) {
            this.lifecycleOwner = lifecycleOwner;
            this.buddyModel = buddyModel;
            this.render = render;
            lifecycleOwner.getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@androidx.annotation.NonNull @NotNull LifecycleOwner source, @androidx.annotation.NonNull @NotNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        source.getLifecycle().removeObserver(this);
                        release();
                    } else if (event == Lifecycle.Event.ON_RESUME) {
                        initRenderAndSink();
                    } else if (event == Lifecycle.Event.ON_STOP) {
                        release();
                    }
                }
            });
            buddyModel.videoTrack.observe(lifecycleOwner, new Observer<VideoTrack>() {
                @Override
                public void onChanged(VideoTrack videoTrack) {
                    if (videoTrack == null) release();
                    else initRenderAndSink();
                }
            });
            if (buddyModel.buddy.isProducer()) {
                cameraFacingState.observe(lifecycleOwner, new Observer<CameraFacingState>() {
                    @Override
                    public void onChanged(CameraFacingState cameraFacingState) {
                        if (TrackBinder.this.render != null && cameraFacingState != CameraFacingState.inProgress) {
                            TrackBinder.this.render.setMirror(cameraFacingState == CameraFacingState.front);
                        }
                    }
                });
            }
        }

        public void release() {
            removeSink();
            if (render != null) {
                render.release();
                ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        render.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }

        public void resetRender(InitSurfaceViewRender render) {
            removeSink();
            this.render = render;
            initRenderAndSink();
        }

        private void initRenderAndSink() {
            VideoTrack videoTrack = buddyModel.videoTrack.getValue();
            if (videoTrack != null && (buddyModel.buddy.isProducer() ? sendTransportState : recvTransportState) != TransportState.disposed) {
                render.init(PeerConnectionUtils.getEglContext(), null);
                videoTrack.addSink(render);
                ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        render.setVisibility(View.VISIBLE);
                    }
                });
                render.setMirror(buddyModel.buddy.isProducer() && cameraFacingState.getValue() == CameraFacingState.front);
            }
        }

        private void removeSink() {
            VideoTrack videoTrack = buddyModel.videoTrack.getValue();
            if (videoTrack != null && (buddyModel.buddy.isProducer() ? sendTransportState : recvTransportState) != TransportState.disposed) {
                videoTrack.removeSink(render);
            }
        }
    }

    public void releaseProducerBuddyRender() {
        for (Map<LifecycleOwner, TrackBinder> map : buddyModelTrackBinderMap.values()) {
            for (TrackBinder trackBinder : map.values()) {
                if (trackBinder.buddyModel.buddy.isProducer()) {
                    trackBinder.release();
                }
            }
        }
    }

    public void releaseConsumerBuddyRender() {
        for (Map<LifecycleOwner, TrackBinder> map : buddyModelTrackBinderMap.values()) {
            for (TrackBinder trackBinder : map.values()) {
                if (!trackBinder.buddyModel.buddy.isProducer()) {
                    trackBinder.release();
                }
            }
        }
    }

    public void bindBuddyRender(LifecycleOwner lifecycleOwner, BuddyModel buddyModel, InitSurfaceViewRender render) {
        if (buddyModel == null) return;
        Map<LifecycleOwner, TrackBinder> map = buddyModelTrackBinderMap.get(buddyModel);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            buddyModelTrackBinderMap.put(buddyModel, map);
        }
        TrackBinder trackBinder = map.get(lifecycleOwner);
        if (trackBinder != null) {
            if (trackBinder.render != render) {
                trackBinder.resetRender(render);
            }
        } else {
            trackBinder = new TrackBinder(lifecycleOwner, buddyModel, render);
            map.put(lifecycleOwner, trackBinder);
        }
    }
    //endregion

    //region 通话组件启动

    public Intent getCallActivityIntent() {
        return new Intent(context, CallRoomActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public Intent getCallServiceIntent() {
        return new Intent(context, CallWindowService.class);
    }

    public Intent getNotificationService() {
        return new Intent(context, CallNotificationService.class);
    }

    public void openCallActivity() {
        context.startActivity(getCallActivityIntent());
    }

    public void openWindowService() {
        context.startService(getCallServiceIntent());
    }

    public void startNotificationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(getNotificationService());
        } else context.startService(getNotificationService());
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

    public void connect(List<User> users) {
        this.firstConnectUsers = users;
        JSONArray jsonArray = new JSONArray();
        if (users != null) {
            for (User user : users) {
                jsonArray.put(user.toJsonObj());
            }
        }
        getRoomClient().connect(jsonArray);
    }

    public void join() {
        if (localConnectionState.getValue() == LocalConnectState.CONNECTED)
            getRoomClient().join();
        else {
            toast("请稍等，连接中...");
        }
    }

    public void hangup() {
        if (callingActual.getValue() == Boolean.FALSE) return;
        stopCallTime();
        callingActual.applySet(false);
        if (callEndFlag == 0) {
            callEndFlag = 1;
            if (owner) {
                callEnd = callingActual.getValue() == null ? CallEnd.Cancel : CallEnd.End;
                localConversationState.applySet(ConversationState.Left);
            } else {
                callEnd = callingActual.getValue() == null ? CallEnd.Reject : CallEnd.End;
                localConversationState.applySet(joinedCount > 0 ? ConversationState.Left : ConversationState.InviteReject);
            }
        } else if (callEndFlag == 2) {
            callEndFlag = 1;
            callEnd = CallEnd.NetError;
            localConversationState.applySet(ConversationState.OfflineTimeout);
        }
        Log.d(TAG, "hangup: before " + callEndFlag + "  " + callEnd);
        if (localConnectionState.getValue() == LocalConnectState.JOINED
                || localConnectionState.getValue() == LocalConnectState.CONNECTED)
            getRoomClient().hangup();
        else {
            getRoomClient().close();
        }
        ArchTaskExecutor.getInstance().postToMainThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "calling.applySet false");
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
        speakerState.applySet(enable ? SpeakerState.on : SpeakerState.off);
    }

    public void switchSpeakerphoneEnable() {
        if (speakerState.getValue() == SpeakerState.on) {
            switchSpeakerphoneEnable(false);
        } else {
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
        Intent intent = uiCallback.getAddUserIntent(context, getRoomOptions().roomId, roomType, audioOnly, list);
        if (intent == null) return;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        UserSelector.start(context, intent);
    }

    public void onAddUserResult(int resultCode, Intent data) {
        if (uiCallback != null) uiCallback.onAddUserResult(resultCode, data);
    }

    @Deprecated
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
                        if (buddyModelMap.get(user.getId()) == null)
                            jsonArray.put(user.toJsonObj());
                    }
                    getRoomClient().addPeers(jsonArray);
                }
            }
        });
    }


    // endregion

    //region 内部使用封装方法 音乐播放 通知栏什么的

    public void setDistanceSensor(AppCompatActivity activity) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP)
            return;
        SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (sensor == null) return;
        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "MSChat:uiRoomStore");
        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] values = event.values;
                if (values != null && event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    if (values[0] == 0) {
                        //贴近手机
                        //关闭屏幕
                        if (!wakeLock.isHeld())
                            wakeLock.acquire();
                    } else {
                        //唤醒设备
                        if (wakeLock.isHeld())
                            wakeLock.release();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        activity.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@androidx.annotation.NonNull LifecycleOwner source, @androidx.annotation.NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_RESUME) {
                    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                } else if (event == Lifecycle.Event.ON_STOP) {
                    sensorManager.unregisterListener(sensorEventListener);
                } else if (event == Lifecycle.Event.ON_DESTROY) {
                    source.getLifecycle().removeObserver(this);
                    sensorManager.unregisterListener(sensorEventListener);
                    if (wakeLock.isHeld())
                        wakeLock.release();
                }
            }
        });
    }

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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.roomOptions, flags);
        dest.writeInt(this.roomType == null ? -1 : this.roomType.ordinal());
        dest.writeByte(this.firstSpeakerOn ? (byte) 1 : (byte) 0);
        dest.writeByte(this.owner ? (byte) 1 : (byte) 0);
        dest.writeByte(this.firstConnectedAutoJoin ? (byte) 1 : (byte) 0);
        dest.writeByte(this.firstJoinedAutoProduceAudio ? (byte) 1 : (byte) 0);
        dest.writeByte(this.firstJoinedAutoProduceVideo ? (byte) 1 : (byte) 0);
        dest.writeByte(this.audioOnly ? (byte) 1 : (byte) 0);
        dest.writeLong(this.createTime != null ? this.createTime.getTime() : -1);
    }

    public void readFromParcel(Parcel source) {
        this.roomOptions = source.readParcelable(RoomOptions.class.getClassLoader());
        int tmpRoomType = source.readInt();
        this.roomType = tmpRoomType == -1 ? null : RoomType.values()[tmpRoomType];
        this.firstSpeakerOn = source.readByte() != 0;
        this.owner = source.readByte() != 0;
        this.firstConnectedAutoJoin = source.readByte() != 0;
        this.firstJoinedAutoProduceAudio = source.readByte() != 0;
        this.firstJoinedAutoProduceVideo = source.readByte() != 0;
        this.audioOnly = source.readByte() != 0;
        long tmpCreateTime = source.readLong();
        this.createTime = tmpCreateTime == -1 ? null : new Date(tmpCreateTime);
    }

    protected UIRoomStore(Parcel in) {
        this.roomOptions = in.readParcelable(RoomOptions.class.getClassLoader());
        int tmpRoomType = in.readInt();
        this.roomType = tmpRoomType == -1 ? null : RoomType.values()[tmpRoomType];
        this.firstSpeakerOn = in.readByte() != 0;
        this.owner = in.readByte() != 0;
        this.firstConnectedAutoJoin = in.readByte() != 0;
        this.firstJoinedAutoProduceAudio = in.readByte() != 0;
        this.firstJoinedAutoProduceVideo = in.readByte() != 0;
        this.audioOnly = in.readByte() != 0;
        long tmpCreateTime = in.readLong();
        this.createTime = tmpCreateTime == -1 ? null : new Date(tmpCreateTime);
    }

    public static final Parcelable.Creator<UIRoomStore> CREATOR = new Parcelable.Creator<UIRoomStore>() {
        @Override
        public UIRoomStore createFromParcel(Parcel source) {
            return new UIRoomStore(source);
        }

        @Override
        public UIRoomStore[] newArray(int size) {
            return new UIRoomStore[size];
        }
    };
}
