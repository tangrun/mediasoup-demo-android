package com.tangrun.mschat;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.model.RoomInfo;
import org.mediasoup.droid.lib.model.RoomState;

import java.util.Random;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 17:08
 */
public class RoomViewModel extends RoomStoreViewModel {

    public MutableLiveData<MultiFragment.Action> left = new MutableLiveData<>();
    public MutableLiveData<MultiFragment.Action> right = new MutableLiveData<>();
    public MutableLiveData<MultiFragment.Action> center = new MutableLiveData<>();
    public MutableLiveData<MultiFragment.Action> bottom = new MutableLiveData<>();
    public MutableLiveData<RoomClient.ConnectionState> connectionState = new MutableLiveData<>(RoomClient.ConnectionState.NEW);
    public MutableLiveData<RoomState.State> micState = new MutableLiveData<>(RoomState.State.Off);
    public MutableLiveData<RoomState.State> camState = new MutableLiveData<>(RoomState.State.Off);
    public MutableLiveData<RoomState.State> speakerState = new MutableLiveData<>(RoomState.State.Off);
    public MutableLiveData<RoomState.State> switchCamState = new MutableLiveData<>(RoomState.State.Off);
    private AudioManager audioManager;
    // 最开始 自动join 一般是房主
    public boolean firstConnectedAutoJoin = false;
    private boolean firstConnectedAutoJoined = false;
    // 首次自动创建音视频流
    public boolean firstJoinedAutoProduceAudio = false;
    public boolean firstJoinedAutoProduceVideo = false;
    private boolean firstJoinedAutoProduced = false;
    private boolean hasJoined = false;// 网络重连时自动连接标记


    public RoomViewModel(@NonNull Application application) {
        super(application);
        audioManager = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);

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

    public void onMinimize() {

    }

    public void onAddBuddy() {
        JSONArray jsonArray = new JSONArray();
        int i = new Random().nextInt(6);
        i = Math.max(i, 1);
        try {
            for (int j = 0; j < 2; j++) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", (j + 1) + "");
                jsonObject.put("displayName", j + " name");
                jsonObject.put("avatar", "");
                jsonArray.put(jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        getRoomClient().addPeers(jsonArray);
    }

    // region 界面音/视频操作

    public void init(LifecycleOwner owner) {
        Log.d("MS_DDDDDDDDDDDDDD", "init: ");
        getRoomStore().getRoomState().observe(owner, new Observer<RoomState>() {
            @Override
            public void onChanged(RoomState roomState) {
                connectionState.setValue(roomState.getConnectionState());
                micState.setValue(roomState.getMicrophoneState());
                camState.setValue(roomState.getCameraState());
                switchCamState.setValue(roomState.getCameraSwitchDeviceState());
            }
        });
        connectionState.observe(owner, connectionState1 -> {
            if (connectionState1 == RoomClient.ConnectionState.CONNECTED) {
                boolean joined = false;
                if (firstConnectedAutoJoin && !firstConnectedAutoJoined) {
                    firstConnectedAutoJoined = true;
                    joined = true;
                    getRoomClient().join();
                }

                if (!joined && hasJoined) {
                    getRoomClient().join();
                }
            } else if (connectionState1 == RoomClient.ConnectionState.JOINED) {
                hasJoined = true;
                if ((firstJoinedAutoProduceAudio || firstJoinedAutoProduceVideo) && !firstJoinedAutoProduced) {
                    firstJoinedAutoProduced = true;
                    if (firstJoinedAutoProduceVideo && camState.getValue() == RoomState.State.Off)
                        switchCamEnable();
                    if (firstJoinedAutoProduceAudio && micState.getValue() == RoomState.State.Off)
                        switchMicEnable();
                }
            }
        });
    }

    private void setSpeakerphoneOn(boolean isSpeakerphoneOn) {
        audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
        audioManager.setMode(isSpeakerphoneOn ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
    }

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

}
