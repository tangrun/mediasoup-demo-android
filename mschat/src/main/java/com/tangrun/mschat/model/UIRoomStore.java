package com.tangrun.mschat.model;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.*;

import androidx.lifecycle.Observer;
import com.example.mschat.R;
import com.example.mschat.databinding.MsLayoutActionBinding;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.enums.RoomType;
import com.tangrun.mschat.ui.UserSelector;
import com.tangrun.mschat.ui.CallWindowService;
import com.tangrun.mschat.ui.CallRoomActivity;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.mediasoup.droid.lib.ArchTaskExecutor;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.WrapperCommon;
import org.mediasoup.droid.lib.enums.*;
import org.mediasoup.droid.lib.lv.*;
import org.mediasoup.droid.lib.model.Buddy;

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

    private String notificationChannelId = null;
    private final int notificationId = 1;
    private final String notificationTag = "UIRoomStore";
    private static final String NOTIFICATION_CHANNEL_ID = "MSCall";
    private static final String NOTIFICATION_CHANNEL_NAME = "音视频通话";

    private final Context context;
    private final AudioManager audioManager;
    private NotificationManagerCompat notificationManagerCompat;
    private AppCompatActivity activity;
    private final RoomStore roomStore;
    private final RoomClient roomClient;
    private final RoomOptions roomOptions;

    public DefaultButtonAction Action_MicrophoneDisabled;
    public DefaultButtonAction Action_CameraDisabled;
    public DefaultButtonAction Action_CameraNotFacing;
    public DefaultButtonAction Action_SpeakerOn;
    public DefaultButtonAction Action_HangupAction;
    public DefaultButtonAction Action_JoinAction;

    /**
     * 自己的状态由自己本地维护
     */
    public ChangedMutableLiveData<ConversationState> localConversationState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<LocalConnectState> localConnectionState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<MicrophoneState> microphoneState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<CameraState> cameraState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<SpeakerState> speakerState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<CameraFacingState> cameraFacingState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<String> callTime = new ChangedMutableLiveData<>();
    /**
     * activity监听这个进行finish
     */
    public ChangedMutableLiveData<Boolean> finished = new ChangedMutableLiveData<>(false);
    public ChangedMutableLiveData<Boolean> showActivity = new ChangedMutableLiveData<>();
    public List<BuddyModel> buddyModels = new ArrayList<>();
    public ChangedMutableLiveData<BuddyModel> mine = new ChangedMutableLiveData<>();
    private Map<String, BuddyModel> buddyModelMap = new ConcurrentHashMap<>();
    public DispatcherObservable<IBuddyModelObserver> buddyObservable = new DispatcherObservable<>(IBuddyModelObserver.class);

    private Date callStartTime, callEndTime;

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
     * 通话已结束标记 0没挂断 1挂断 2超时
     */
    private int callEndType = 0;


    /**
     * 连接状态改变监听
     */
    Observer<LocalConnectState> localConnectionStateChangedLogic = connectionState1 -> {
        Log.d(TAG, "ConnectionState changed: " + connectionState1);
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

            // 不是邀请者 且还没join过
            if (!owner && joinedCount == 0) {
                localConversationState.setValue(ConversationState.Invited);
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

            localConversationState.setValue(ConversationState.Joined);

            joinedCount++;
        } else if (connectionState1 == LocalConnectState.CLOSED) {
            if (callEndType == 1) {
                if (owner)
                    localConversationState.setValue(ConversationState.Left);
                else {
                    if (joinedCount > 0) {
                        localConversationState.setValue(ConversationState.Left);
                    } else {
                        localConversationState.setValue(ConversationState.InviteReject);
                    }
                }
            } else if (callEndType == 2) {
                localConversationState.setValue(ConversationState.InviteTimeout);
            }
            if (callEndType != 0)
                release();
        }
    };

    Observable<Long> volumeDelaySilentObservable = Observable.timer(1200, TimeUnit.MILLISECONDS);
    Map<String, DisposableObserver<Long>> volumeDelaySilentObserverMap = new ConcurrentHashMap<>();

    //region 监听数据变化并经过转化设置到本store成员上

    ClientObserver clientObserver = new ClientObserver() {
        @Override
        public void onBuddyAdd(String id, Buddy buddy) {
            ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    BuddyModel buddyModel = new BuddyModel(buddy);
                    buddyModel.connectionState.applyPost(buddy.getConnectionState());
                    buddyModel.conversationState.applyPost(buddy.getConversationState());
                    buddyModel.volume.applyPost(null);
                    buddyModel.disabledCam.applyPost(!audioOnly);
                    buddyModel.disabledMic.applyPost(true);

                    buddyModels.add(buddyModel);
                    int pos = buddyModels.indexOf(buddyModel);
                    buddyModelMap.put(id, buddyModel);

                    buddyObservable.getDispatcher().onBuddyAdd(pos, buddyModel);

                    if (mine.getValue() == null && buddy.isProducer()){
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

                    if (callStartTime != null && getRoomStore().getBuddys().size() == 1) {
                        hangup();
                    }
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
                        startCallTime();
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
                return;
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
            // 自己接听就算开始通话
            if (callStartTime == null && !owner && state == LocalConnectState.JOINED && joinedCount == 0) {
                startCallTime();
            }
        }

        @Override
        public void onCameraStateChanged(CameraState state) {
            cameraState.applyPost(state);
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

    LifecycleEventObserver appProcessObserver = new LifecycleEventObserver() {
        @Override
        public void onStateChanged(@androidx.annotation.NonNull @NotNull LifecycleOwner source, @androidx.annotation.NonNull @NotNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                source.getLifecycle().removeObserver(this);
            } else if (event == Lifecycle.Event.ON_RESUME) {
                updateNotification();
            } else if (event == Lifecycle.Event.ON_STOP) {
                openWindowService();
            }
        }
    };

    LifecycleEventObserver activityObserver = new LifecycleEventObserver() {
        @Override
        public void onStateChanged(@androidx.annotation.NonNull @NotNull LifecycleOwner source, @androidx.annotation.NonNull @NotNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_RESUME){
                showActivity.applyPost(true);
            }else if (event == Lifecycle.Event.ON_DESTROY){
                source.getLifecycle().removeObserver(this);
                activity = null;
            }
        }
    };

    //endregion


    public UIRoomStore(Context context, RoomOptions roomOptions) {
        this.context = context;
        this.roomClient = new RoomClient(context, roomOptions);
        this.roomStore = roomClient.getStore();
        this.roomOptions = roomClient.getOptions();
        audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        notificationManagerCompat = NotificationManagerCompat.from(context);
        init();
    }

    public void bindLifeOwner(AppCompatActivity owner) {
        if (activity != null)return;
        activity = owner;
        activity.getLifecycle().addObserver(activityObserver);
        cameraState.observe(activity, state -> Action_CameraDisabled.setChecked(state == CameraState.disabled));
        microphoneState.observe(activity, state -> Action_MicrophoneDisabled.setChecked(state == MicrophoneState.disabled));
        cameraFacingState.observe(activity, state -> Action_CameraNotFacing.setChecked(state == CameraFacingState.rear));
        speakerState.observe(activity, state -> Action_SpeakerOn.setChecked(state == SpeakerState.on));
    }

    private void init() {
        {
            localConversationState.applyPost(ConversationState.New);
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
            Action_JoinAction = new DefaultButtonAction("", audioOnly ? R.drawable.selector_call_audio_answer : R.drawable.selector_call_video_answer) {
                @Override
                public void onClick(View v) {
                    join();
                }
            };
            Action_HangupAction = new DefaultButtonAction("", R.drawable.selector_call_hangup) {
                @Override
                public void onClick(View v) {
                    hangup();
                    v.postDelayed(() -> {
                        finished.applySet(true);
                        MSManager.stopCall();
                    }, 1000);
                }
            };
            Action_MicrophoneDisabled = new DefaultButtonAction("麦克风", R.drawable.ms_mic_disabled) {
                @Override
                public void onClick(View v) {
                    switchMicEnable(v.getContext());
                }
            };
            Action_SpeakerOn = new DefaultButtonAction("免提", R.drawable.ms_speaker_on) {
                @Override
                public void onClick(View v) {
                    switchSpeakerphoneEnable();
                }
            };
            Action_CameraDisabled = new DefaultButtonAction("摄像头", R.drawable.ms_cam_disabled) {
                @Override
                public void onClick(View v) {
                    switchCamEnable(v.getContext());
                }
            };
            Action_CameraNotFacing = new DefaultButtonAction("切换摄像头", R.drawable.ms_cam_changed) {
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
        localConversationState.observeForever(conversationState1 -> {
            updateNotification();
        });
        //
        localConnectionState.observeForever(localConnectionStateChangedLogic);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(appProcessObserver);
        //
        finished.observeForever(aBoolean -> {
            if (aBoolean) {
                release();
                MSManager.stopCall();
            }
        });

        getRoomStore().getClientObservable().registerObserver(clientObserver);
    }

    private void updateNotification() {
        ConversationState conversationState = localConversationState.getValue();
        LocalConnectState connectState = localConnectionState.getValue();
        if (conversationState == ConversationState.New) {
            setNotification("等待对方接听");
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
        localConnectionState.removeObserver(localConnectionStateChangedLogic);
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(appProcessObserver);
        getRoomStore().getClientObservable().unregisterAll();
        stopCallTime();
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
        if (callEndTime != null) return;
        if (callTimeObserver != null)
            callTimeObserver.dispose();
        callTimeObserver = null;
        callEndTime = new Date();
    }

    //endregion

    // region 功能操作暴露方法

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
        callEndType = 1;
        if (localConnectionState.getValue() == LocalConnectState.JOINED
                || localConnectionState.getValue() == LocalConnectState.CONNECTED)
            getRoomClient().hangup();
        else {
            getRoomClient().close();
        }
        ArchTaskExecutor.getInstance().postToMainThread(new Runnable() {
            @Override
            public void run() {
                finished.applySet(true);
            }
        }, 1500);
    }

    public void switchMicEnable(Context context) {
        if (!XXPermissions.isGranted(context, Permission.RECORD_AUDIO)) {
            showDialog(context, "权限请求", "通话中语音需要录音权限，请授予",
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
            showDialog(context, "权限请求", "通话中视频需要摄像头权限，请授予", "取消", null, "好的", new Runnable() {
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
        openWindowService();
    }

    public void onAddUserClick(Context context) {
        List<MSManager.User> list = new ArrayList<>();
        for (Buddy buddy : getRoomStore().getBuddys().values()) {
            MSManager.User user = new MSManager.User();
            user.setId(buddy.getId());
            user.setAvatar(buddy.getAvatar());
            user.setDisplayName(buddy.getDisplayName());
            list.add(user);
        }
        UserSelector.start(context, list);
    }

    public void addUser(List<MSManager.User> list) {
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
                    for (MSManager.User user : list) {
                        jsonArray.put(user.toJsonObj());
                    }
                    getRoomClient().addPeers(jsonArray);
                }
            }
        });
    }


    // endregion

    //region 封装UI方法

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
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(title).setMessage(msg);
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
