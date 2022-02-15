package com.tangrun.mschat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peers;
import org.mediasoup.droid.lib.model.RoomInfo;
import org.mediasoup.droid.lib.model.RoomState;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 17:08
 */
public class RoomViewModel extends RoomStoreViewModel {

    private MultiFragment.Action left ;
    private MultiFragment.Action right ;
    private MultiFragment.Action center ;
    private MultiFragment.Action bottom ;
    private RoomInfo roomInfo ;
    private RoomClient.ConnectionState connectionState ;
    private RoomState.State micState ;
    private RoomState.State camState ;
    private RoomState.State speakerState ;
    private RoomState.State switchCamState ;


    public RoomViewModel(@NonNull Application application) {
        super(application);
    }

    public void onMic() {
        if (micState == RoomState.State.Off)
            getRoomClient().enableMic();
        else if (micState == RoomState.State.On)
            getRoomClient().disableMic();
    }

    public void onCam() {
        if (camState == RoomState.State.Off)
            getRoomClient().enableCam();
        else if (camState== RoomState.State.On)
            getRoomClient().disableCam();
    }

    public void onMin() {

    }

    public void onAdd() {

    }

    public RoomState.State getMicState() {
        return micState;
    }

    public RoomViewModel setMicState(RoomState.State micState) {
        this.micState = micState;
        return this;
    }

    public RoomState.State getCamState() {
        return camState;
    }

    public RoomViewModel setCamState(RoomState.State camState) {
        this.camState = camState;
        return this;
    }

    public RoomState.State getSpeakerState() {
        return speakerState;
    }

    public RoomViewModel setSpeakerState(RoomState.State speakerState) {
        this.speakerState = speakerState;
        return this;
    }

    public RoomState.State getSwitchCamState() {
        return switchCamState;
    }

    public RoomViewModel setSwitchCamState(RoomState.State switchCamState) {
        this.switchCamState = switchCamState;
        return this;
    }

    public RoomClient.ConnectionState getConnectionState() {
        return connectionState;
    }

    public RoomViewModel setConnectionState(RoomClient.ConnectionState connectionState) {
        this.connectionState = connectionState;
        return this;
    }

    public MultiFragment.Action getLeft() {
        return left;
    }

    public RoomViewModel setLeft(MultiFragment.Action left) {
        this.left = left;
        return this;
    }

    public MultiFragment.Action getRight() {
        return right;
    }

    public RoomViewModel setRight(MultiFragment.Action right) {
        this.right = right;
        return this;
    }

    public MultiFragment.Action getCenter() {
        return center;
    }

    public RoomViewModel setCenter(MultiFragment.Action center) {
        this.center = center;
        return this;
    }

    public MultiFragment.Action getBottom() {
        return bottom;
    }

    public RoomViewModel setBottom(MultiFragment.Action bottom) {
        this.bottom = bottom;
        return this;
    }

    public RoomInfo getRoomInfo() {
        return roomInfo;
    }

    public RoomViewModel setRoomInfo(RoomInfo roomInfo) {
        this.roomInfo = roomInfo;
        return this;
    }
}
