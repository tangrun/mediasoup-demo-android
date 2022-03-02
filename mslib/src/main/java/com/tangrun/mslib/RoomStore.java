package com.tangrun.mslib;

import android.util.Log;
import com.tangrun.mslib.enums.*;
import com.tangrun.mslib.lv.ClientObserver;
import com.tangrun.mslib.lv.DispatcherObservable;
import com.tangrun.mslib.lv.SupplierMutableLiveData;
import com.tangrun.mslib.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Producer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room state.
 *
 * <p>Just like mediasoup-demo/app/lib/redux/stateActions.js
 */
@SuppressWarnings("unused")
public class RoomStore {

    private static final String TAG = "RoomStore";

    private final DispatcherObservable<ClientObserver> clientObservable = new DispatcherObservable<>(ClientObserver.class);

    private LocalConnectState localConnectionState = LocalConnectState.NEW.NEW;
    private CameraState cameraState = CameraState.disabled;
    private MicrophoneState microphoneState = MicrophoneState.disabled;
    private CameraFacingState cameraFacingState = CameraFacingState.front;
    private final Map<String, Buddy> buddys = new ConcurrentHashMap<>();
    private final Map<String, WrapperCommon<?>> wrappers = new ConcurrentHashMap<>();


    public void addNotify(String msg) {
        Log.d(TAG, "addNotify: " + msg);
    }

    public void addNotify(String tag, String msg) {
        Log.d(TAG, "addNotify: " + tag + " : " + msg);
    }

    public DispatcherObservable<ClientObserver> getClientObservable() {
        return clientObservable;
    }


    public LocalConnectState getLocalConnectionState() {
        return localConnectionState;
    }

    public CameraState getCameraState() {
        return cameraState;
    }

    public MicrophoneState getMicrophoneState() {
        return microphoneState;
    }

    public CameraFacingState getCameraFacingState() {
        return cameraFacingState;
    }

    public Map<String, Buddy> getBuddys() {
        return buddys;
    }

    public Map<String, WrapperCommon<?>> getWrappers() {
        return wrappers;
    }

    // region roomState

    public void setLocalConnectionState(LocalConnectState localConnectionState) {
        this.localConnectionState = localConnectionState;
        clientObservable.getDispatcher().onLocalConnectStateChanged(localConnectionState);
    }

    public void setCameraState(CameraState cameraState) {
        this.cameraState = cameraState;
        clientObservable.getDispatcher().onCameraStateChanged(cameraState);
    }

    public void setMicrophoneState(MicrophoneState microphoneState) {
        this.microphoneState = microphoneState;
        clientObservable.getDispatcher().onMicrophoneStateChanged(microphoneState);
    }

    public void setCameraFacingState(CameraFacingState cameraFacingState) {
        this.cameraFacingState = cameraFacingState;
        clientObservable.getDispatcher().onCameraFacingChanged(cameraFacingState);
    }

    // endregion


    // region producer
    public void setWrapperScore(String id, Integer producerScore, Integer consumerScore) {
        getWrapperPost(id, value -> {
            value.setProducerScore(producerScore);
            value.setConsumerScore(consumerScore);
            getBuddyPost(value.getBuddyId(), value1 -> clientObservable.getDispatcher()
                    .onProducerScoreChanged(value.getBuddyId(), value1, id, value));
        });
    }

    public void setWrapperResumed(String id, Originator originator) {
        getWrapperPost(id, value -> {
            if (Originator.local == originator) {
                value.setLocallyPaused(false);
            } else {
                value.setRemotelyPaused(false);
            }
            getBuddyPost(value.getBuddyId(), value1 -> clientObservable.getDispatcher()
                    .onProducerResumed(value.getBuddyId(), value1, id, value));
        });
    }

    public void setWrapperPaused(String id, Originator originator) {
        getWrapperPost(id, value -> {
            if (Originator.local == originator) {
                value.setLocallyPaused(true);
            } else {
                value.setRemotelyPaused(true);
            }
            getBuddyPost(value.getBuddyId(), value1 -> clientObservable.getDispatcher()
                    .onProducerPaused(value.getBuddyId(), value1, id, value));
        });
    }

    public void addWrapper(boolean producer, String buddyId, String id, String kind, Object data) {
        WrapperCommon<?> wrapperCommon;
        if (producer) {
            wrapperCommon = new ProducerWrapper(buddyId, id, kind, (Producer) data);
        } else {
            wrapperCommon = new ConsumerWrapper(buddyId, id, kind, (Consumer) data);
        }
        wrappers.put(wrapperCommon.getId(), wrapperCommon);

        getBuddyPost(buddyId, value -> clientObservable.getDispatcher()
                .onProducerAdd(value.getId(), value, id, wrapperCommon));
    }

    public void removeWrapper(boolean needClose, String producerId) {
        WrapperCommon<?> wrapperCommon = wrappers.remove(producerId);
        if (wrapperCommon != null) {
            if (needClose)
                wrapperCommon.close();
            getBuddyPost(wrapperCommon.getBuddyId(), value -> clientObservable.getDispatcher()
                    .onProducerRemove(value.getId(), value, producerId));
        }
    }
    // endregion


    // region buddy
    public Buddy getBuddy(String id) {
        return buddys.get(id);
    }

    public void removeBuddy(String peerId) {
        Buddy buddy = buddys.remove(peerId);
        if (buddy == null) {
            return;
        }
        clientObservable.getDispatcher().onBuddyRemove(peerId);
    }

    public void addBuddy(Buddy buddy) {
        Buddy oldBuddy = buddys.get(buddy.getId());
        if (oldBuddy == null) {
            buddys.put(buddy.getId(), buddy);
            clientObservable.getDispatcher().onBuddyAdd(buddy.getId(), buddy);
            oldBuddy = buddy;
        } else {
            oldBuddy.setConnectionState(buddy.getConnectionState());
            oldBuddy.setConversationState(buddy.getConversationState());
        }
        clientObservable.getDispatcher().onBuddyStateChanged(oldBuddy.getId(), oldBuddy);
    }

    public void addBuddyForPeers(JSONArray jsonArray) {
        List<Buddy> list = new ArrayList<>();
        for (int i = 0; jsonArray != null && i < jsonArray.length(); i++) {
            try {
                JSONObject peer = jsonArray.getJSONObject(i);
                Buddy buddy = new Buddy(false, peer);
                list.add(buddy);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!list.isEmpty()) {
            for (Buddy buddy : list) {
                addBuddy(buddy);
            }
        }
    }

    public void addBuddyForPeer(String id, JSONObject jsonObject) {
        addBuddy(new Buddy(false, jsonObject));
    }

    public void addBuddyForMe(String id, String name, String avatar, DeviceInfo deviceInfo) {
        addBuddy(new Buddy(true, id, name, avatar, deviceInfo));
    }

    public void updateBuddy(String buddyId, JSONObject jsonObject) {
        getBuddyPost(buddyId, value -> {
            Buddy buddy = new Buddy(false, jsonObject);
            value.setConnectionState(buddy.getConnectionState());
            value.setConversationState(buddy.getConversationState());
            clientObservable.getDispatcher().onBuddyStateChanged(buddyId, value);
        });
    }

    // endregion

    public void setSpeakerSilent() {
        for (Buddy buddy : buddys.values()) {
            if (buddy.getVolume() != null && buddy.getVolume() != 0) {
                buddy.setVolume(0);
                clientObservable.getDispatcher().onBuddyVolumeChanged(buddy.getId(), buddy);
            }
        }
    }

    public void setSpeakerVolume(String buddyId, int volume) {
        getBuddyPost(buddyId, value -> {
            value.setVolume(volume);
            clientObservable.getDispatcher().onBuddyVolumeChanged(value.getId(), value);
        });
    }


    private void getBuddyPost(String buddyId, SupplierMutableLiveData.Invoker<Buddy> invoker) {
        Buddy buddy = getBuddy(buddyId);
        if (buddy != null && invoker != null) {
            invoker.invokeAction(buddy);
        }
    }

    private void getWrapperPost(String id, SupplierMutableLiveData.Invoker<WrapperCommon<?>> invoker) {
        WrapperCommon<?> wrapper = wrappers.get(id);
        if (wrapper != null && invoker != null) {
            invoker.invokeAction(wrapper);
        }
    }
}
