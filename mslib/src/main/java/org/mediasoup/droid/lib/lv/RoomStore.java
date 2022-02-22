package org.mediasoup.droid.lib.lv;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.WrapperCommon;
import org.mediasoup.droid.lib.model.Buddy;
import org.mediasoup.droid.lib.model.Buddys;
import org.mediasoup.droid.lib.model.DeviceInfo;
import org.mediasoup.droid.lib.model.RoomState;
import org.mediasoup.droid.lib.model.Producers;
import org.mediasoup.droid.lib.model.Consumers;

import java.util.ArrayList;
import java.util.List;

/**
 * Room state.
 *
 * <p>Just like mediasoup-demo/app/lib/redux/stateActions.js
 */
@SuppressWarnings("unused")
public class RoomStore {

    private static final String TAG = "RoomStore";


    private final SupplierMutableLiveData<RoomState> roomState = new SupplierMutableLiveData<>(RoomState::new);
    private final SupplierMutableLiveData<Buddys> buddys = new SupplierMutableLiveData<>(Buddys::new);
    private final Producers producers = new Producers();
    private final Consumers consumers = new Consumers();

    public SupplierMutableLiveData<RoomState> getRoomState() {
        return roomState;
    }

    public Producers getProducers() {
        return producers;
    }

    public Consumers getConsumers() {
        return consumers;
    }

    public void addNotify(String msg) {
        Log.d(TAG, "addNotify: " + msg);
    }

    public void addNotify(String tag, String msg) {
        Log.d(TAG, "addNotify: " + tag + " : " + msg);
    }


    // region roomState
    public void setRestartIceState(RoomState.State state) {
        roomState.postValue(value -> value.setRestartIceState(state));
    }

    public void setCameraSwitchDeviceState(RoomState.State state) {
        roomState.postValue(value -> value.setCameraSwitchDeviceState(state));
    }

    public void setMicrophoneState(RoomState.State state) {
        roomState.postValue(value -> value.setMicrophoneState(state));
    }

    public void setCameraState(RoomState.State state) {
        roomState.postValue(value -> value.setCameraState(state));
    }

    public void setConnectionState(RoomClient.ConnectionState state) {
        roomState.postValue(value -> value.setConnectionState(state));
    }
    // endregion


    // region producer
    public void setProducerScore(String peerId, String producerId, JSONArray score) {
        if (producers.setProducerScore(producerId, score)) {
            getBuddyPost(peerId, value -> {

            });
        }
    }

    public void setProducerResumed(String producerId) {
        producers.setProducerResumed(producerId);
    }

    public void setProducerPaused(String producerId) {
        producers.setProducerPaused(producerId);
    }

    public void addProducer(String peerId, Producer producer) {
        producers.addProducer(producer);
        getBuddyPost(peerId, value -> value.getIds().add(producer.getId()));
    }

    public void removeProducer(String peerId, String producerId) {
        producers.removeProducer(producerId);
        getBuddyPost(peerId, value -> value.getIds().remove(producerId));
    }
    // endregion


    // region consumer

    public void setConsumerCurrentLayers(String consumerId, int spatialLayer, int temporalLayer) {
        consumers.setConsumerCurrentLayers(consumerId, spatialLayer, temporalLayer);
    }

    public void setConsumerScore(String peerId, String consumerId, JSONObject score) {
        if (consumers.setConsumerScore(consumerId, score)) {
            getBuddyPost(peerId, value -> {

            });
        }
    }

    public void setConsumerResumed(String consumerId, String type) {
        consumers.setConsumerResumed(consumerId, type);
    }

    public void setConsumerPaused(String consumerId, String type) {
        consumers.setConsumerPaused(consumerId, type);
    }

    public void addConsumer(String peerId, String type, Consumer consumer, boolean remotelyPaused) {
        consumers.addConsumer(type, consumer, remotelyPaused);
        getBuddyPost(peerId, value -> value.getIds().add(consumer.getId()));
    }

    public void removeConsumer(String peerId, String consumerId) {
        consumers.removeConsumer(consumerId);
        getBuddyPost(peerId, value -> {
            value.getIds().remove(consumerId);
        });
    }
    // endregion

    // region buddy
    public Buddy getBuddy(String id) {
        return buddys.getValue().getBuddy(id);
    }

    public void removeBuddy(String peerId) {
        buddys.postValue(value -> value.remove(peerId));
    }

    public void addBuddy(Buddy buddy) {
        buddys.postValue(value -> {
            value.addBuddy(buddy);
        });
    }

    public void addBuddyForPeers(JSONArray jsonArray) {
        List<Buddy> list = new ArrayList<>();
        try {
            for (int i = 0; jsonArray != null && i < jsonArray.length(); i++) {
                JSONObject peer = jsonArray.getJSONObject(i);
                Buddy buddy = new Buddy(false, peer);
                list.add(buddy);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!list.isEmpty()) {
            buddys.postValue(value -> {
                for (Buddy buddy : list) {
                    value.addBuddy(buddy);
                }
            });
        }
    }

    public void addBuddyForPeer(String id, JSONObject jsonObject) {
        addBuddy(new Buddy(false, jsonObject));
    }

    public void addBuddyForMe(String id, String name, String avatar, DeviceInfo deviceInfo) {
        addBuddy(new Buddy(true, id, name, avatar, deviceInfo));
    }

    public void updateBuddy(String peerId, JSONObject jsonObject) {
        getBuddyPost(peerId, value -> {
            Buddy buddy = new Buddy(false, jsonObject);
            value.setConnectionState(buddy.getConnectionState());
            value.setConversationState(buddy.getConversationState());
        });
    }

    // endregion

    public void setSpeakerVolume(JSONArray jsonArray) {
        if (jsonArray == null) {
            for (Buddy buddy : buddys.getValue().getAllPeers()) {
                if (buddy.getVolume() == null) continue;
                buddy.getBuddyLiveData().postValue(value1 -> {
                    value1.setVolume(null);
                });
            }
        } else {
            try {
                for (int i = 0, j = jsonArray.length(); i < j; i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String peerId = jsonObject.optString("peerId");
                    int volume = jsonObject.optInt("volume");
                    getBuddyPost(peerId, value -> {
                        value.setVolume(volume);
                    });
                }
            } catch (Exception e) {

            }
        }
    }


    private void getBuddyPost(String buddyId, SupplierMutableLiveData.Invoker<Buddy> invoker) {
        Buddy buddy = getBuddy(buddyId);
        if (buddy != null) {
            buddy.getBuddyLiveData().postValue(invoker);
        }
    }

    private void getConsumerPost(String consumerId, SupplierMutableLiveData.Invoker<Consumers.ConsumerWrapper> invoker) {
        Consumers.ConsumerWrapper wrapper = consumers.getConsumer(consumerId);
        if (wrapper != null) {
            wrapper.getWrapperCommonLiveData().postValue(new SupplierMutableLiveData.Invoker<WrapperCommon>() {
                @Override
                public void invokeAction(WrapperCommon value) {
                    invoker.invokeAction(wrapper);
                }
            });
        }
    }

    public SupplierMutableLiveData<Buddys> getBuddys() {
        return buddys;
    }


}
