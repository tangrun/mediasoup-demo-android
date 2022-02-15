package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.Constant;
import org.mediasoup.droid.lib.TrackInvoker;
import org.mediasoup.droid.lib.WrapperCommon;
import org.mediasoup.droid.lib.lv.SupplierMutableLiveData;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Producers implements TrackInvoker {

    @Override
    public AudioTrack getAudioTrack(Collection<String> ids) {
        for (String id : ids) {
            ProducersWrapper wrapper = getProducer(id);
            if (wrapper != null && Constant.kind_audio.equals(wrapper.getType())) {
                if (wrapper.getProducer() != null)
                    return (AudioTrack) wrapper.getProducer().getTrack();
            }
        }
        return null;
    }

    @Override
    public VideoTrack getVideoTrack(Collection<String> ids) {
        for (String id : ids) {
            ProducersWrapper wrapper = getProducer(id);
            if (wrapper != null && Constant.kind_video.equals(wrapper.getType())) {
                if (wrapper.getProducer() != null)
                    return (VideoTrack) wrapper.getProducer().getTrack();
            }
        }
        return null;
    }

    public static class ProducersWrapper extends WrapperCommon {

        public static final String TYPE_CAM = "cam";
        public static final String TYPE_SHARE = "share";

        private Producer mProducer;
        private String mType;

        /**
         * 属性变化
         * score paused resumed
         */
        private final SupplierMutableLiveData<WrapperCommon> producersWrapperSupplierMutableLiveData;

        ProducersWrapper(Producer producer) {
            this.mProducer = producer;
            producersWrapperSupplierMutableLiveData = new SupplierMutableLiveData<>(() -> ProducersWrapper.this);
        }

        public SupplierMutableLiveData<WrapperCommon> getProducersWrapperSupplierMutableLiveData() {
            return producersWrapperSupplierMutableLiveData;
        }

        public Producer getProducer() {
            return mProducer;
        }

        public JSONArray getScore() {
            return mScore;
        }

        public String getType() {
            return mType;
        }

        private void setLocallyPaused(boolean b) {
            mLocallyPaused = b;
        }

        private void setRemotelyPaused(boolean b) {
            mRemotelyPaused = b;
        }

        private void setScore(JSONArray jsonArray) {
            mScore = jsonArray;
        }


    }

    private final Map<String, ProducersWrapper> mProducers;

    public Producers() {
        mProducers = new ConcurrentHashMap<>();
    }

    public void addProducer(Producer producer) {
        mProducers.put(producer.getId(), new ProducersWrapper(producer));
    }

    public void removeProducer(String producerId) {
        mProducers.remove(producerId);
    }

    public void setProducerPaused(String producerId) {
        ProducersWrapper wrapper = mProducers.get(producerId);
        if (wrapper == null) {
            return;
        }

        wrapper.getProducersWrapperSupplierMutableLiveData().postValue(value -> {
            wrapper.setRemotelyPaused(false);
            wrapper.mProducer.pause();
        });
    }

    public void setProducerResumed(String producerId) {
        ProducersWrapper wrapper = mProducers.get(producerId);
        if (wrapper == null) {
            return;
        }

        wrapper.getProducersWrapperSupplierMutableLiveData().postValue(value -> {
                    wrapper.setLocallyPaused(false);
                    wrapper.mProducer.resume();

                }
        );
    }

    public void setProducerScore(String producerId, JSONArray score) {
        ProducersWrapper wrapper = mProducers.get(producerId);
        if (wrapper == null) {
            return;
        }

        wrapper.getProducersWrapperSupplierMutableLiveData().postValue(value -> wrapper.setScore(score));
    }

    public ProducersWrapper getProducer(String producerId) {
        return mProducers.get(producerId);
    }

    public void clear() {
        mProducers.clear();
    }
}
