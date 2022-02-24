package com.tangrun.mschat;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;

import com.example.mschat.R;
import com.example.mschat.databinding.ItemActionBinding;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.lv.ChangedMutableLiveData;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Buddy;
import org.mediasoup.droid.lib.model.Buddys;
import org.mediasoup.droid.lib.model.RoomState;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private final Application application;
    private final RoomStore roomStore;
    private final RoomClient roomClient;
    private final RoomOptions roomOptions;

    DefaultButtonAction Action_MicDisabledAction;
    DefaultButtonAction Action_CamDisabledAction;
    DefaultButtonAction Action_CamNotIsFrontAction;
    DefaultButtonAction Action_SpeakerOnAction;
    DefaultButtonAction Action_HangupAction;
    DefaultButtonAction Action_JoinAction;


    /**
     * 自己的状态由自己本地维护
     */
    public ChangedMutableLiveData<Buddy.ConversationState> conversationState = new ChangedMutableLiveData<>(Buddy.ConversationState.New);
    public ChangedMutableLiveData<RoomClient.ConnectionState> connectionState = new ChangedMutableLiveData<>(RoomClient.ConnectionState.NEW);
    public ChangedMutableLiveData<RoomState.State> micEnabledState = new ChangedMutableLiveData<>(RoomState.State.Off);
    public ChangedMutableLiveData<RoomState.State> camEnabledState = new ChangedMutableLiveData<>(RoomState.State.Off);
    public ChangedMutableLiveData<RoomState.State> speakerOnState = new ChangedMutableLiveData<>(RoomState.State.Off);
    public ChangedMutableLiveData<RoomState.State> CamIsFrontState = new ChangedMutableLiveData<>(RoomState.State.Off);
    public ChangedMutableLiveData<Long> callTime = new ChangedMutableLiveData<>(null);
    public ChangedMutableLiveData<Boolean> finished = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Boolean> showActivity = new ChangedMutableLiveData<>(true);
    public ChangedMutableLiveData<List<BuddyItemViewModel>> buddys = new ChangedMutableLiveData<>(new ArrayList<>());
    private final AudioManager audioManager;
    private LifecycleOwner lifecycleOwner;
    private Date callStart;

    private int connectedCount;
    private int joinedCount;

    /**
     * 0 单人
     * 1 多人
     * 默认0 单人
     */
    public int roomType;
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
     * 首次连接自动创建音频流 m
     * 默认 false
     */
    public boolean firstJoinedAutoProduce = false;
    /**
     * 这个只在自动创建音视频流时使用
     * 实际的上传 接收流设置
     *
     * @see RoomOptions
     */
    public boolean audioOnly = true;
    /**
     * 通话已结束标记 1挂断 2超时
     */
    private int callEnd = 0;

    Observer<RoomState> roomStateObserver = new Observer<RoomState>() {
        @Override
        public void onChanged(RoomState roomState) {
            connectionState.applySet(roomState.getConnectionState());
            micEnabledState.applySet(roomState.getMicrophoneEnabledState());
            camEnabledState.applySet(roomState.getCameraEnabledState());
            CamIsFrontState.applySet(roomState.getCameraIsFrontDeviceState());
        }
    };
    Observer<RoomClient.ConnectionState> localConnectionStateChangedLogic = connectionState1 -> {
        Log.d(TAG, "ConnectionState changed: " + connectionState1);
        if (connectionState1 == RoomClient.ConnectionState.CONNECTED) {
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
                conversationState.setValue(Buddy.ConversationState.Invited);
            }
            connectedCount++;
        } else if (connectionState1 == RoomClient.ConnectionState.JOINED) {
            // 网络中断重连时 join后 重连transport
            // 用重连transport无效 因为socket重连后是新的对象 之前的数据都没了 所以只能根据自己本地的状态判断去在重连上后主动传流
            if (joinedCount > 0) {
                if (camEnabledState.getValue() == RoomState.State.On) {
                    getRoomClient().enableCam();
                }
                if (micEnabledState.getValue() == RoomState.State.On) {
                    getRoomClient().enableMic();
                }
            }


            // 首次join后 自动发送流
            if (firstJoinedAutoProduce && joinedCount == 0) {
                ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleEventObserver() {
                    @Override
                    public void onStateChanged(@androidx.annotation.NonNull @NotNull LifecycleOwner source, @androidx.annotation.NonNull @NotNull Lifecycle.Event event) {
                        if (event == Lifecycle.Event.ON_RESUME) {
                            source.getLifecycle().removeObserver(this);
                            if (lifecycleOwner != null) {
                                lifecycleOwner.getLifecycle().addObserver(new LifecycleEventObserver() {
                                    @Override
                                    public void onStateChanged(@androidx.annotation.NonNull @NotNull LifecycleOwner source, @androidx.annotation.NonNull @NotNull Lifecycle.Event event) {
                                        if (event == Lifecycle.Event.ON_RESUME) {
                                            source.getLifecycle().removeObserver(this);
                                            Context context = null;
                                            if (source instanceof Context) {
                                                context = (Context) source;
                                            } else if (source instanceof Fragment) {
                                                context = ((Fragment) source).getContext();
                                            }
                                            if (context != null) {
                                                if (!audioOnly && camEnabledState.getValue() == RoomState.State.Off)
                                                    switchCamEnable(context);
                                                if (micEnabledState.getValue() == RoomState.State.Off)
                                                    switchMicEnable(context);
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                });

            }

            conversationState.setValue(Buddy.ConversationState.Joined);

            joinedCount++;
        } else if (connectionState1 == RoomClient.ConnectionState.CLOSED) {
            if (callEnd == 1) {
                if (owner)
                    conversationState.setValue(Buddy.ConversationState.Left);
                else {
                    if (joinedCount > 0) {
                        conversationState.setValue(Buddy.ConversationState.Left);
                    } else {
                        conversationState.setValue(Buddy.ConversationState.InviteReject);
                    }
                }
            } else if (callEnd == 2) {
                conversationState.setValue(Buddy.ConversationState.InviteTimeout);
            }
            if (callEnd != 0)
                release();
        }
    };
    Observer<Buddy> buddyObserver = new Observer<Buddy>() {
        @Override
        public void onChanged(Buddy buddy) {
            if (callStart == null) {
                if (!buddy.isProducer() &&
                        buddy.getConnectionState() == Buddy.ConnectionState.Online && buddy.getConversationState() == Buddy.ConversationState.Joined) {
                    callStart = new Date();
                    startCallTime();
                }
            }
            for (BuddyItemViewModel model : buddys.getValue()) {
                if (model.buddy.getId().equals(buddy.getId())) {
                    model.onChanged(buddy);
                    break;
                }
            }
        }
    };
    Observer<Buddys> buddysObserver = new Observer<Buddys>() {
        @Override
        public void onChanged(Buddys buddys) {
            List<Buddy> allPeers = buddys.getAllPeers();
            List<BuddyItemViewModel> itemViewModels = new ArrayList<>();
            for (Buddy peer : allPeers) {
                peer.getBuddyLiveData().removeObserver(buddyObserver);
                peer.getBuddyLiveData().observeForever(buddyObserver);
                BuddyItemViewModel model = new BuddyItemViewModel(peer, getRoomClient());
                itemViewModels.add(model);
            }
            UIRoomStore.this.buddys.applySet(itemViewModels);
        }
    };

    public UIRoomStore(Application application, RoomClient roomClient) {
        this.application = application;
        this.roomClient = roomClient;
        this.roomStore = roomClient.getStore();
        this.roomOptions = roomClient.getOptions();
        audioManager = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
        init();
    }

    public void bindLifeOwner(LifecycleOwner owner) {
        lifecycleOwner = owner;
        camEnabledState.observe(lifecycleOwner, state -> Action_CamDisabledAction.setChecked(state == RoomState.State.Off));
        micEnabledState.observe(lifecycleOwner, state -> Action_MicDisabledAction.setChecked(state == RoomState.State.Off));
        CamIsFrontState.observe(lifecycleOwner, state -> Action_CamNotIsFrontAction.setChecked(state == RoomState.State.Off));
        speakerOnState.observe(lifecycleOwner, state -> Action_SpeakerOnAction.setChecked(state == RoomState.State.On));
    }

    private DisposableObserver<Long> callTimeObserver = new DisposableObserver<Long>() {
        @Override
        public void onNext(@NonNull Long aLong) {
            callTime.applyPost(System.currentTimeMillis() - callStart.getTime());
        }

        @Override
        public void onError(@NonNull Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };

    private void startCallTime() {
        callTimeObserver.dispose();
        Observable.interval(0, 1, TimeUnit.SECONDS)
                .subscribe(callTimeObserver);
    }

    private void stopCallTime() {
        callTimeObserver.dispose();
    }

    private void startWindowService(Context context) {
        context.startService(new Intent(context, CallWindowService.class));
    }

    private void init() {
        getRoomStore().getRoomState().observeForever(roomStateObserver);
        getRoomStore().getBuddys().observeForever(buddysObserver);
        connectionState.observeForever(localConnectionStateChangedLogic);
        showActivity.observeForever(new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    MSManager.openCallActivity(application);
                } else {
                    startWindowService(application);
                }
            }
        });
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
        Action_MicDisabledAction = new DefaultButtonAction("麦克风", R.drawable.ms_mic_disabled) {
            @Override
            public void onClick(View v) {
                switchMicEnable(v.getContext());
            }
        };
        Action_SpeakerOnAction = new DefaultButtonAction("免提", R.drawable.ms_speaker_on) {
            @Override
            public void onClick(View v) {
                switchSpeakerphoneEnable();
            }
        };
        Action_CamDisabledAction = new DefaultButtonAction("摄像头", R.drawable.ms_cam_disabled) {
            @Override
            public void onClick(View v) {
                switchCamEnable(v.getContext());
            }
        };
        Action_CamNotIsFrontAction = new DefaultButtonAction("切换摄像头", R.drawable.ms_cam_changed) {
            @Override
            public void onClick(View v) {
                switchCamDevice();
            }
        };
    }

    private void release() {
        getRoomStore().getRoomState().removeObserver(roomStateObserver);
        getRoomStore().getBuddys().removeObserver(buddysObserver);
        connectionState.removeObserver(localConnectionStateChangedLogic);
    }

    private void toast(String text) {
        Toast.makeText(application, text, Toast.LENGTH_SHORT).show();
    }

    public void join() {
        if (connectionState.getValue() == RoomClient.ConnectionState.CONNECTED)
            getRoomClient().join();
        else {
            toast("请稍等，连接中...");
        }
    }

    public void hangup() {
        callEnd = 1;
        if (connectionState.getValue() == RoomClient.ConnectionState.JOINED
                || connectionState.getValue() == RoomClient.ConnectionState.CONNECTED)
            getRoomClient().hangup();
        else {
            getRoomClient().close();
        }
    }

    public void addBuddy(List<MSManager.User> list) {
        if (list == null || list.isEmpty()) return;
        connectionState.observeForever(new Observer<RoomClient.ConnectionState>() {
            @Override
            public void onChanged(RoomClient.ConnectionState state) {
                if (state == RoomClient.ConnectionState.CONNECTED) {
                    connectionState.removeObserver(this);
                    JSONArray jsonArray = new JSONArray();
                    for (MSManager.User user : list) {
                        jsonArray.put(user.toJsonObj());
                    }
                    getRoomClient().addPeers(jsonArray);
                }
            }
        });
    }

    // region 界面音/视频操作

    interface PermissionCallback extends OnPermissionCallback {
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
        if (micEnabledState.getValue() == RoomState.State.Off)
            getRoomClient().enableMic();
        else if (micEnabledState.getValue() == RoomState.State.On)
            getRoomClient().disableMic();
    }

    public void switchCamEnable(Context context) {
        if (!XXPermissions.isGranted(context, Permission.CAMERA)) {
            showDialog(context, "权限请求", "通话中视频需要摄像头权限，请授予", "取消", null, "好的", new Runnable() {
                @Override
                public void run() {
                    XXPermissions.with(context).permission(Permission.CAMERA).request((PermissionCallback) (all, never) -> {
                        if (all) {
                            switchMicEnable(context);
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
        if (camEnabledState.getValue() == RoomState.State.Off)
            getRoomClient().enableCam();
        else if (camEnabledState.getValue() == RoomState.State.On)
            getRoomClient().disableCam();
    }

    private void setSpeakerphoneOn(boolean isSpeakerphoneOn) {
        audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
        audioManager.setMode(isSpeakerphoneOn ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
    }

    private void switchSpeakerphoneEnable(boolean enable) {
        setSpeakerphoneOn(enable);
        speakerOnState.setValue(enable ? RoomState.State.On : RoomState.State.Off);
    }

    public void switchSpeakerphoneEnable() {
        if (speakerOnState.getValue() == RoomState.State.On) {
            switchSpeakerphoneEnable(false);
        } else if (camEnabledState.getValue() == RoomState.State.Off) {
            switchSpeakerphoneEnable(true);
        }
    }

    public void switchCamDevice() {
        if (camEnabledState.getValue() != RoomState.State.On) return;
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
        activity.finish();
    }

    public void onAddUserClick(Context context) {
        List<Buddy> allPeers = getRoomStore().getBuddys().getValue().getAllPeers();
        List<MSManager.User> list = new ArrayList<>();
        for (Buddy buddy : allPeers) {
            MSManager.User user = new MSManager.User();
            user.setId(buddy.getId());
            user.setAvatar(buddy.getAvatar());
            user.setDisplayName(buddy.getDisplayName());
            list.add(user);
        }
        AddUserHandler.start(context, list);
    }
    // endregion


    public RoomStore getRoomStore() {
        return roomStore;
    }

    public RoomClient getRoomClient() {
        return roomClient;
    }

    public RoomOptions getRoomOptions() {
        return roomOptions;
    }


    public static abstract class ButtonAction<V> implements View.OnClickListener {
        protected String name;
        protected int imgId;
        protected boolean checked;
        protected V v;

        public abstract void bindView(V v);

        public abstract void setChecked(boolean checked);


    }

    public abstract static class DefaultButtonAction extends ButtonAction<ItemActionBinding> {


        public DefaultButtonAction(String name, int imgId) {
            this.name = name;
            this.imgId = imgId;
        }

        @Override
        public void bindView(ItemActionBinding itemActionBinding) {
            this.v = itemActionBinding;
            if (itemActionBinding == null) return;
            itemActionBinding.llContent.setVisibility(View.VISIBLE);
            itemActionBinding.llContent.setOnClickListener(this);
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
