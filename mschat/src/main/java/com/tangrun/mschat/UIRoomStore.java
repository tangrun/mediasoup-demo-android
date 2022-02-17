package com.tangrun.mschat;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import org.json.JSONArray;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.lv.ChangedMutableLiveData;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Buddys;
import org.mediasoup.droid.lib.model.RoomState;

import java.util.List;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/17 10:51
 */
public class UIRoomStore {

    private static final String TAG = "MS_UIRoomStore";

    private RoomStore roomStore;
    private RoomClient roomClient;
    private RoomOptions roomOptions;

    public ChangedMutableLiveData<RoomClient.ConnectionState> connectionState = new ChangedMutableLiveData<>(RoomClient.ConnectionState.NEW);
    public ChangedMutableLiveData<RoomState.State> micState = new ChangedMutableLiveData<>(RoomState.State.Off);
    public ChangedMutableLiveData<RoomState.State> camState = new ChangedMutableLiveData<>(RoomState.State.Off);
    public ChangedMutableLiveData<RoomState.State> speakerState = new ChangedMutableLiveData<>(RoomState.State.Off);
    public ChangedMutableLiveData<RoomState.State> switchCamState = new ChangedMutableLiveData<>(RoomState.State.Off);
    public ChangedMutableLiveData<RoomState.State> restIceState = new ChangedMutableLiveData<>(RoomState.State.Off);
    private AudioManager audioManager;
    /**
     * 0 单人
     * 1 多人
     * 默认0 单人
     */
    public int roomType;

    /**
     * 最开始 自动join 一般是房主
     * 默认 false
     */
    public boolean firstConnectedAutoJoin = false;
    private boolean firstConnectedAutoJoined = false;
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
    private boolean firstJoinedAutoProduced = false;
    private boolean hasJoined = false;// 网络重连时自动连接标记

    Observer<RoomState> roomStateObserver = new Observer<RoomState>() {
        @Override
        public void onChanged(RoomState roomState) {
            connectionState.applySet(roomState.getConnectionState());
            micState.applySet(roomState.getMicrophoneState());
            camState.applySet(roomState.getCameraState());
            switchCamState.applySet(roomState.getCameraSwitchDeviceState());
            switchCamState.applySet(roomState.getCameraSwitchDeviceState());
            restIceState.applySet(roomState.getRestartIceState());
        }
    };
    Observer<RoomClient.ConnectionState> localConnectionStateChangedLogic = connectionState1 -> {
        Log.d(TAG, "ConnectionState changed: "+connectionState1);
        if (connectionState1 == RoomClient.ConnectionState.CONNECTED) {
            boolean needJoin = false;

            // 首次连接上 自动join
            if (firstConnectedAutoJoin && !firstConnectedAutoJoined) {
                firstConnectedAutoJoined = true;
                needJoin = true;
            }

            // 重连时自动join
            if (hasJoined){
                needJoin = true;
            }

            if (needJoin) {
                getRoomClient().join();
            }
        } else if (connectionState1 == RoomClient.ConnectionState.JOINED) {
            // 网络中断重连时 join后 重连transport
            // 用重连transport无效 因为socket重连后是新的对象 之前的数据都没了 所以只能根据自己本地的状态判断去在重连上后主动传流
            if (hasJoined){
                //getRoomClient().restartIce();
                if (camState.getValue() == RoomState.State.On){
                    getRoomClient().enableCam();
                }
                if (micState.getValue() == RoomState.State.On){
                    getRoomClient().enableMic();
                }
            }
            hasJoined = true;

            // 首次join后 自动发送流
            if (firstJoinedAutoProduce && !firstJoinedAutoProduced) {
                firstJoinedAutoProduced = true;
                if (!audioOnly && camState.getValue() == RoomState.State.Off)
                    switchCamEnable();
                if (micState.getValue() == RoomState.State.Off)
                    switchMicEnable();
            }
        }
    };

    public void addBuddyChangeObserve(LifecycleOwner owner, Observer<Buddys> buddysObserver) {
        getRoomStore().getBuddys().observe(owner, buddysObserver);
    }

    public UIRoomStore(Application application, RoomClient roomClient) {
        this.roomClient = roomClient;
        this.roomStore = roomClient.getStore();
        this.roomOptions = roomClient.getOptions();
        audioManager = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
        init();
    }

    private void init() {
        getRoomStore().getRoomState().observeForever(roomStateObserver);
        connectionState.observeForever(localConnectionStateChangedLogic);
    }

    private void release() {
        getRoomStore().getRoomState().removeObserver(roomStateObserver);
        connectionState.removeObserver(localConnectionStateChangedLogic);
    }

    public void join() {
        if (connectionState.getValue() == RoomClient.ConnectionState.CONNECTED)
            getRoomClient().join();
    }

    public void hangup() {
        if (connectionState.getValue() == RoomClient.ConnectionState.JOINED
                || connectionState.getValue() == RoomClient.ConnectionState.CONNECTED)
            getRoomClient().hangup();
        else {
            getRoomClient().close();
        }
    }

    public void addBuddy(List<Start.User> list) {
        if (list == null || list.isEmpty()) return;
        connectionState.observeForever(new Observer<RoomClient.ConnectionState>() {
            @Override
            public void onChanged(RoomClient.ConnectionState state) {
                if (state == RoomClient.ConnectionState.CONNECTED) {
                    connectionState.removeObserver(this);
                    JSONArray jsonArray = new JSONArray();
                    for (Start.User user : list) {
                        jsonArray.put(user.toJsonObj());
                    }
                    getRoomClient().addPeers(jsonArray);
                }
            }
        });
    }

    // region 界面音/视频操作

    public void switchMicEnable() {
        if (micState.getValue() == RoomState.State.Off)
            getRoomClient().enableMic();
        else if (micState.getValue() == RoomState.State.On)
            getRoomClient().disableMic();
    }

    public void switchCamEnable() {
        if (camState.getValue() == RoomState.State.Off)
            getRoomClient().enableCam();
        else if (camState.getValue() == RoomState.State.On)
            getRoomClient().disableCam();
    }

    private void setSpeakerphoneOn(boolean isSpeakerphoneOn) {
        audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
        audioManager.setMode(isSpeakerphoneOn ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
    }

    public void switchSpeakerphoneEnable() {
        if (speakerState.getValue() == RoomState.State.On) {
            setSpeakerphoneOn(false);
            speakerState.setValue(RoomState.State.Off);
        } else if (camState.getValue() == RoomState.State.Off) {
            setSpeakerphoneOn(true);
            speakerState.setValue(RoomState.State.On);
        }
    }

    public void switchCamDevice() {
        if (camState.getValue() != RoomState.State.On) return;
        getRoomClient().changeCam();
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
}
