package com.tangrun.mschat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.lv.RoomStore;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 17:08
 */
public class RoomViewModel extends RoomStoreViewModel {


    public RoomViewModel(@NonNull Application application) {
        super(application);
    }


}
