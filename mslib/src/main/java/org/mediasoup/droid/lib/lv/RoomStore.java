package org.mediasoup.droid.lib.lv;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.Constant;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.WrapperCommon;
import org.mediasoup.droid.lib.model.Buddy;
import org.mediasoup.droid.lib.model.DeviceInfo;

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

    private final BuddyObservable buddyObservable = new BuddyObservable();

    private Constant.ConnectionState connectionState = Constant.ConnectionState.NEW;
    private Constant.CameraState cameraState = Constant.CameraState.disabled;
    private Constant.MicrophoneState microphoneState = Constant.MicrophoneState.disabled;
    private Constant.CameraFacingState cameraFacingState = Constant.CameraFacingState.front;
    private final Map<String, Buddy> buddys = new ConcurrentHashMap<>();
    private final Map<String, ConsumerWrapper> consumers = new ConcurrentHashMap<>();
    private final Map<String, ProducerWrapper> producers = new ConcurrentHashMap<>();
    private final Map<String, WrapperCommon<?>> wrappers = new ConcurrentHashMap<>();


    public void addNotify(String msg) {
        Log.d(TAG, "addNotify: " + msg);
    }

    public void addNotify(String tag, String msg) {
        Log.d(TAG, "addNotify: " + tag + " : " + msg);
    }

    public BuddyObservable getBuddyObservable() {
        return buddyObservable;
    }


    public Constant.ConnectionState getConnectionState() {
        return connectionState;
    }

    public Constant.CameraState getCameraState() {
        return cameraState;
    }

    public Constant.MicrophoneState getMicrophoneState() {
        return microphoneState;
    }

    public Constant.CameraFacingState getCameraFacingState() {
        return cameraFacingState;
    }

    public Map<String, Buddy> getBuddys() {
        return buddys;
    }

    public Map<String, ConsumerWrapper> getConsumers() {
        return consumers;
    }

    public Map<String, ProducerWrapper> getProducers() {
        return producers;
    }

    public Map<String, WrapperCommon<?>> getWrappers() {
        return wrappers;
    }

    // region roomState

    public void setConnectionState(Constant.ConnectionState connectionState) {
        this.connectionState = connectionState;
    }

    public void setCameraState(Constant.CameraState cameraState) {
        this.cameraState = cameraState;
    }

    public void setMicrophoneState(Constant.MicrophoneState microphoneState) {
        this.microphoneState = microphoneState;
    }

    public void setCameraFacingState(Constant.CameraFacingState cameraFacingState) {
        this.cameraFacingState = cameraFacingState;
    }

    // endregion


    // region producer
    public void setWrapperScore(String id, Integer producerScore, Integer consumerScore) {
        getWrapperPost(id, value -> {
            value.setProducerScore(producerScore);
            value.setConsumerScore(consumerScore);
            getBuddyPost(value.getBuddyId(), value1 -> buddyObservable.dispatcher()
                    .onProducerScoreChanged(value.getBuddyId(), value1, id, value));
        });
    }

    public void setWrapperResumed(String id, Constant.Originator originator) {
        getWrapperPost(id, value -> {
            if (Constant.Originator.local== originator) {
                value.setLocallyPaused(false);
            } else {
                value.setRemotelyPaused(false);
            }
            getBuddyPost(value.getBuddyId(), value1 -> buddyObservable.dispatcher()
                    .onProducerResumed(value.getBuddyId(), value1, id, value));
        });
    }

    public void setWrapperPaused(String id, Constant.Originator originator) {
        getWrapperPost(id, value -> {
            if (Constant.Originator.local == originator) {
                value.setLocallyPaused(true);
            } else {
                value.setRemotelyPaused(true);
            }
            getBuddyPost(value.getBuddyId(), value1 -> buddyObservable.dispatcher()
                    .onProducerPaused(value.getBuddyId(), value1, id, value));
        });
    }

    public void addWrapper(boolean producer, String buddyId, String id, String kind, Object data) {
        WrapperCommon<?> wrapperCommon;
        if (producer) {
            ProducerWrapper producerWrapper = new ProducerWrapper(buddyId, id, kind, (Producer) data);
            producers.put(producerWrapper.getId(), producerWrapper);
            wrapperCommon = producerWrapper;
        } else {
            ConsumerWrapper consumerWrapper = new ConsumerWrapper(buddyId, id, kind, (Consumer) data);
            consumers.put(consumerWrapper.getId(), consumerWrapper);
            wrapperCommon = consumerWrapper;
        }

        getBuddyPost(buddyId, value -> buddyObservable.dispatcher()
                .onProducerAdd(value.getId(), value, id, wrapperCommon));
    }

    public void removeWrapper(boolean needClose, String producerId) {
        WrapperCommon<?> wrapperCommon = wrappers.remove(producerId);
        if (wrapperCommon != null) {
            wrapperCommon.close();
            getBuddyPost(wrapperCommon.getBuddyId(), value -> buddyObservable.dispatcher()
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
        buddyObservable.dispatcher().onBuddyRemove(peerId);
    }

    public void addBuddy(Buddy buddy) {
        Buddy oldBuddy = buddys.put(buddy.getId(), buddy);
        if (oldBuddy == null) {
            buddyObservable.dispatcher().onBuddyAdd(buddy.getId(), buddy);
        }
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
            buddyObservable.dispatcher().onBuddyStateChanged(buddyId, value);
        });
    }

    // endregion

    public void setSpeakerSilent() {
        for (Buddy buddy : buddys.values()) {
            if (buddy.getVolume() != null) {
                buddy.setVolume(null);
                buddyObservable.dispatcher().onBuddyVolumeChanged(buddy.getId(), buddy);
            }
        }
    }

    public void setSpeakerVolume(String buddyId, Integer volume) {
        getBuddyPost(buddyId, value -> {
            value.setVolume(volume);
            buddyObservable.dispatcher().onBuddyVolumeChanged(value.getId(), value);
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
