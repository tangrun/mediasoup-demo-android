package com.tangrun.mslib.lv;

import com.tangrun.mslib.model.Buddy;

public interface IBuddyListener{
    void onBuddyAdd(String id, Buddy buddy);
    void onBuddyRemove(String id);
    void onBuddyVolumeChanged(String id, Buddy buddy);
    void onBuddyStateChanged(String id, Buddy buddy);
}
