package org.mediasoup.droid.lib;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.enums.Originator;
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
                mStore.removeWrapper(false,consumerId);
                break;
            }
            case "consumerPaused": {
                String consumerId = data.getString("consumerId");
                mStore.setWrapperPaused(consumerId, Originator.remote);
                break;
            }
            case "consumerResumed": {
                String consumerId = data.getString("consumerId");
                mStore.setWrapperResumed(consumerId, Originator.remote);
                break;
            }
            case "consumerLayersChanged": {
                String consumerId = data.getString("consumerId");
                int spatialLayer = data.optInt("spatialLayer");
                int temporalLayer = data.optInt("temporalLayer");
//                mStore.setConsumerCurrentLayers(consumerId, spatialLayer, temporalLayer);
                break;
            }
            case "producerScore": {
                String peerId = data.getString("peerId");
                String producerId = data.getString("producerId");
                JSONArray score = data.getJSONArray("score");
                try {
                    JSONObject jsonObject = score.getJSONObject(0);
                    int scoreNum = jsonObject.optInt("score");
                    mStore.setWrapperScore(producerId,scoreNum,null);
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            }
            case "consumerScore": {
                String peerId = data.getString("peerId");
                String consumerId = data.getString("consumerId");
                JSONObject score = data.optJSONObject("score");
                try {
                    int producerScore = score.optInt("producerScore");
                    int consumerScore = score.optInt("score");
                    mStore.setWrapperScore(consumerId,producerScore,consumerScore);
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            }
            case "activeSpeaker": {
                JSONArray jsonArray = data.optJSONArray("volumes");
                if (jsonArray ==null){
                    mStore.setSpeakerSilent();
                }else {
                    for (int i = 0, j = jsonArray.length(); i < j; i++) {
                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String peerId = jsonObject.optString("peerId");
                            int volume = jsonObject.optInt("volume");
                            mStore.setSpeakerVolume(peerId,volume);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            }
            default: {
                Logger.e(TAG, "unknown protoo notification.method " + notification.getMethod());
            }
        }
    }
}
