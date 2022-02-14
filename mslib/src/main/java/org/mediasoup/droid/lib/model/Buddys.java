package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

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

    public void addMe(String peerId, String displayName, String avatar, DeviceInfo deviceInfo) {
        mPeersInfo.put(peerId, new Buddy(true, peerId, displayName, avatar, deviceInfo));
    }

    public void addBuddy(Buddy buddy) {
        mPeersInfo.put(buddy.getId(), buddy);
    }

    public void addPeer(String peerId, @NonNull JSONObject peerInfo) {
        mPeersInfo.put(peerId, new Buddy(false, peerInfo));
    }

    public void remove(String id) {
        mPeersInfo.remove(id);
    }


    public void addId(String buddyId, String id) {
        Buddy buddy = getBuddy(buddyId);
        if (buddy == null) {
            Logger.e(TAG, "no Peer found for new Consumer");
            return;
        }

        buddy.getIds().add(id);
    }

    public void removeId(String peerId, String consumerId) {
        Buddy buddy = getBuddy(peerId);
        if (buddy == null) {
            return;
        }

        buddy.getIds().remove(consumerId);
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
