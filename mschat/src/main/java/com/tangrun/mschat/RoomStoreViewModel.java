package com.tangrun.mschat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.lv.RoomStore;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 10:24
 */
public class RoomStoreViewModel extends AndroidViewModel {

    private  RoomStore roomStore;
    private RoomClient roomClient;
    private RoomOptions roomOptions;

    public RoomStoreViewModel(@NonNull Application application) {
        super(application);
    }

    public void setRoomStore( RoomClient roomClient) {
        this.roomClient = roomClient;
        this.roomStore = roomClient.getStore();
        this.roomOptions = roomClient.getOptions();
    }

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
