package org.mediasoup.droid.lib.model;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.lib.Constant;
import org.mediasoup.droid.lib.TrackInvoker;
import org.mediasoup.droid.lib.WrapperCommon;
import org.mediasoup.droid.lib.lv.SupplierMutableLiveData;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Consumers implements TrackInvoker {

    @Override
    public AudioTrack getAudioTrack(Collection<String> ids) {
        for (String id : ids) {
            ConsumerWrapper wrapper = getConsumer(id);
            if (wrapper != null && wrapper.getConsumer() !=null && Constant.kind_audio.equals(wrapper.getConsumer().getKind())) {
                    return (AudioTrack) wrapper.getConsumer().getTrack();
            }
        }
        return null;
    }

    @Override
    public VideoTrack getVideoTrack(Collection<String> ids) {
        for (String id : ids) {
            ConsumerWrapper wrapper = getConsumer(id);
            if (wrapper != null && wrapper.getConsumer() !=null && Constant.kind_video.equals(wrapper.getConsumer().getKind())) {
                    return (VideoTrack) wrapper.getConsumer().getTrack();
            }
        }
        return null;
    }

    public static class ConsumerWrapper extends WrapperCommon {

        private String mType;
        private int mSpatialLayer;
        private int mTemporalLayer;
        private Consumer mConsumer;
        private int mPreferredSpatialLayer;
        private int mPreferredTemporalLayer;

        /**
         * 属性变化
         * score paused resumed
         */
        SupplierMutableLiveData<WrapperCommon> consumerWrapperSupplierMutableLiveData;

        ConsumerWrapper(String type, boolean remotelyPaused, Consumer consumer) {
            mType = type;
            mLocallyPaused = false;
            mRemotelyPaused = remotelyPaused;
            mSpatialLayer = -1;
            mTemporalLayer = -1;
            mConsumer = consumer;
            mPreferredSpatialLayer = -1;
            mPreferredTemporalLayer = -1;
            consumerWrapperSupplierMutableLiveData = new SupplierMutableLiveData<>(ConsumerWrapper.this);
        }

        public SupplierMutableLiveData<WrapperCommon> getConsumerWrapperSupplierMutableLiveData() {
            return consumerWrapperSupplierMutableLiveData;
        }

        public String getType() {
            return mType;
        }

        public int getSpatialLayer() {
            return mSpatialLayer;
        }

        public int getTemporalLayer() {
            return mTemporalLayer;
        }

        public Consumer getConsumer() {
            return mConsumer;
        }

        public int getPreferredSpatialLayer() {
            return mPreferredSpatialLayer;
        }

        public int getPreferredTemporalLayer() {
            return mPreferredTemporalLayer;
        }

        private void setLocallyPaused(boolean b){
            mLocallyPaused = b;
        }

        private void setRemotelyPaused(boolean b){
            mRemotelyPaused = b;
        }

        private void setScore(JSONArray jsonArray){
            mScore = jsonArray;
        }
    }

    private final Map<String, ConsumerWrapper> consumers;

    public Consumers() {
        consumers = new ConcurrentHashMap<>();
    }

    public void addConsumer(String type, Consumer consumer, boolean remotelyPaused) {
        consumers.put(consumer.getId(), new ConsumerWrapper(type, remotelyPaused, consumer));
    }

    public void removeConsumer(String consumerId) {
        consumers.remove(consumerId);
    }

    public void setConsumerPaused(String consumerId, String originator) {
        ConsumerWrapper wrapper = consumers.get(consumerId);
        if (wrapper == null) {
            return;
        }

        wrapper.getConsumerWrapperSupplierMutableLiveData().postValue(value -> {
            if (Constant.originator_local.equals(originator)) {
                wrapper.setLocallyPaused(true);
            } else {
                wrapper.setRemotelyPaused(true);
            }
        });
    }

    public void setConsumerResumed(String consumerId, String originator) {
        ConsumerWrapper wrapper = consumers.get(consumerId);
        if (wrapper == null) {
            return;
        }

        wrapper.getConsumerWrapperSupplierMutableLiveData().postValue(value -> {
            if (Constant.originator_local.equals(originator)) {
                wrapper.setLocallyPaused(false);
            } else {
                wrapper.setRemotelyPaused(false);
            }
        });
    }


    public void setConsumerScore(String consumerId, JSONArray score) {
        ConsumerWrapper wrapper = consumers.get(consumerId);
        if (wrapper == null) {
            return;
        }

        wrapper.getConsumerWrapperSupplierMutableLiveData().postValue(value -> wrapper.setScore(score));
    }

    public void setConsumerCurrentLayers(String consumerId, int spatialLayer, int temporalLayer) {
        ConsumerWrapper wrapper = consumers.get(consumerId);
        if (wrapper == null) {
            return;
        }

        wrapper.getConsumerWrapperSupplierMutableLiveData().postValue(value -> {
            wrapper.mSpatialLayer = spatialLayer;
            wrapper.mTemporalLayer = temporalLayer;
        });

    }

    public ConsumerWrapper getConsumer(String consumerId) {
        return consumers.get(consumerId);
    }

    public void clear() {
        consumers.clear();
    }
}
