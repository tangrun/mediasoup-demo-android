package org.mediasoup.droid.lib;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.protoojs.droid.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RoomMessageHandler {

    static final String TAG = "MS_RoomClient";

    // Stored Room States.
    @NonNull
    final RoomStore mStore;
    // mediasoup Consumers.
    @NonNull
    final Map<String, ConsumerHolder> mConsumers;

    static class ConsumerHolder {
        @NonNull
        final String peerId;
        @NonNull
        final Consumer mConsumer;

        ConsumerHolder(@NonNull String peerId, @NonNull Consumer consumer) {
            this.peerId = peerId;
            mConsumer = consumer;
        }
    }

    RoomMessageHandler(@NonNull RoomStore store) {
        this.mStore = store;
        this.mConsumers = new ConcurrentHashMap<>();
    }

    @WorkerThread
    void handleNotification(Message.Notification notification) throws JSONException {
        JSONObject data = notification.getData();
        switch (notification.getMethod()) {
            case "newPeer": {
                String id = data.getString("id");
                mStore.addBuddyForPeer(id, data);
                break;
            }
            case "peerUpdate":{
                String peerId = data.optString("peerId");
                JSONObject jsonObject = data.optJSONObject("peerInfo");
                mStore.updateBuddy(peerId, jsonObject);
                break;
            }
            case "newPeers": {
                JSONArray jsonArray = data.optJSONArray("peers");
                mStore.addBuddyForPeers( jsonArray);
                break;
            }
            case "peerClosed": {
                String peerId = data.getString("peerId");
                mStore.removeBuddy(peerId);
                break;
            }
            case "consumerClosed": {
                String consumerId = data.getString("consumerId");
                ConsumerHolder holder = mConsumers.remove(consumerId);
                if (holder == null) {
                    break;
                }
                holder.mConsumer.close();
                mConsumers.remove(consumerId);
                mStore.removeConsumer(holder.peerId, holder.mConsumer.getId());
                break;
            }
            case "consumerPaused": {
                String consumerId = data.getString("consumerId");
                ConsumerHolder holder = mConsumers.get(consumerId);
                if (holder == null) {
                    break;
                }
                mStore.setConsumerPaused(holder.mConsumer.getId(), Constant.originator_remote);
                break;
            }
            case "consumerResumed": {
                String consumerId = data.getString("consumerId");
                ConsumerHolder holder = mConsumers.get(consumerId);
                if (holder == null) {
                    break;
                }
                mStore.setConsumerResumed(holder.mConsumer.getId(), Constant.originator_remote);
                break;
            }
            case "consumerLayersChanged": {
                String consumerId = data.getString("consumerId");
                int spatialLayer = data.optInt("spatialLayer");
                int temporalLayer = data.optInt("temporalLayer");
                ConsumerHolder holder = mConsumers.get(consumerId);
                if (holder == null) {
                    break;
                }
                mStore.setConsumerCurrentLayers(consumerId, spatialLayer, temporalLayer);
                break;
            }
            case "producerScore": {
                String peerId = data.getString("peerId");
                String producerId = data.getString("producerId");
                JSONArray score = data.getJSONArray("score");
                mStore.setProducerScore(peerId,producerId, score);
                break;
            }
            case "consumerScore": {
                String peerId = data.getString("peerId");
                String consumerId = data.getString("consumerId");
                JSONObject score = data.optJSONObject("score");
                ConsumerHolder holder = mConsumers.get(consumerId);
                if (holder == null) {
                    break;
                }
                mStore.setConsumerScore(peerId,consumerId, score);
                break;
            }
            case "activeSpeaker": {
                JSONArray jsonArray = data.optJSONArray("volumes");
                mStore.setSpeakerVolume(jsonArray);
                break;
            }
            default: {
                Logger.e(TAG, "unknown protoo notification.method " + notification.getMethod());
            }
        }
    }
}
