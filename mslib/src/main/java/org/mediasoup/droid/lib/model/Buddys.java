package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Buddys {

    private static final String TAG = "Peers";

    private Map<String, Buddy> mPeersInfo;

    public Buddys() {
        mPeersInfo = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public void addBuddy(Buddy buddy) {
        mPeersInfo.put(buddy.getId(), buddy);
    }

    public void remove(String id) {
        mPeersInfo.remove(id);
    }

    public void setSpeakerVolume(String buddyId, Integer volume){
        Buddy buddy = getBuddy(buddyId);
        if (buddy == null) {
            Logger.e(TAG, "no Peer found for new Consumer");
            return;
        }

        buddy.getBuddyMutableLiveData().postValue(value -> value.setVolume(volume));
    }

    public void addId(String buddyId, String id) {
        Buddy buddy = getBuddy(buddyId);
        if (buddy == null) {
            Logger.e(TAG, "no Peer found for new Consumer");
            return;
        }

        buddy.getBuddyMutableLiveData().postValue(value -> value.getIds().add(id));
    }

    public void removeId(String buddyId, String consumerId) {
        Buddy buddy = getBuddy(buddyId);
        if (buddy == null) {
            return;
        }

        buddy.getBuddyMutableLiveData().postValue(value -> value.getIds().remove(consumerId));
    }

    public Buddy getBuddy(String peerId) {
        return mPeersInfo.get(peerId);
    }

    public List<Buddy> getAllPeers() {
        List<Buddy> peers = new ArrayList<>();
        for (Map.Entry<String, Buddy> info : mPeersInfo.entrySet()) {
            peers.add(info.getValue());
        }
        return peers;
    }

    public void clear() {
        mPeersInfo.clear();
    }
}
