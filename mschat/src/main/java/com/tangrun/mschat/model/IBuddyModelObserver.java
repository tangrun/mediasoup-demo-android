package com.tangrun.mschat.model;

public interface IBuddyModelObserver {
    void onBuddyAdd(int position,BuddyModel buddyModel);

    void onBuddyRemove(int position,BuddyModel buddyModel);
}
