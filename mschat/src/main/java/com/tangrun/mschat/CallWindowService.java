package com.tangrun.mschat;

import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.Observer;

public class CallWindowService extends LifecycleService {

    UIRoomStore uiRoomStore;

    @Override
    public void onCreate() {
        super.onCreate();
        uiRoomStore = MSManager.getCurrent();

        uiRoomStore.showActivity.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    stopSelf();
                }
            }
        });
    }
}
