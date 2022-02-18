package org.mediasoup.droid.lib.model;

import androidx.lifecycle.LiveData;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.Constant;
import org.mediasoup.droid.lib.CommonInvoker;
import org.mediasoup.droid.lib.WrapperCommon;
import org.mediasoup.droid.lib.lv.SupplierMutableLiveData;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStreamTrack;
import org.webrtc.VideoTrack;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Producers implements CommonInvoker {

    @Override
    public WrapperCommon getCommonInfo(Collection<String> ids, String kind) {
        for (String id : ids) {
            ProducersWrapper wrapper = getProducer(id);
            if (wrapper != null && wrapper.getProducer() != null && kind.equals(wrapper.getProducer().getKind())) {
                return wrapper;
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
            producersWrapperSupplierMutableLiveData = new SupplierMutableLiveData<>(ProducersWrapper.this);
        }

        @Override
        public SupplierMutableLiveData<WrapperCommon> getWrapperCommonLiveData() {
            return producersWrapperSupplierMutableLiveData;
        }

        public Producer getProducer() {
            return mProducer;
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

        private void setConsumerScore(int score) {
            mConsumerScore = score;
        }

        private void setProducerScore(int score) {
            mProducerScore = score;
        }

        @Override
        public <T extends MediaStreamTrack> T getTrack() {
            return mProducer == null ? null : (T) mProducer.getTrack();
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

//        wrapper.getWrapperCommonLiveData().postValue(value -> {
            wrapper.setRemotelyPaused(false);
            wrapper.mProducer.pause();
//        });
    }

    public void setProducerResumed(String producerId) {
        ProducersWrapper wrapper = mProducers.get(producerId);
        if (wrapper == null) {
            return;
        }

//        wrapper.getWrapperCommonLiveData().postValue(value -> {
                    wrapper.setLocallyPaused(false);
                    wrapper.mProducer.resume();

//                });
    }

    public boolean setProducerScore(String producerId, JSONArray score) {
        if (score == null || score.length() == 0) return false;
        ProducersWrapper wrapper = mProducers.get(producerId);
        if (wrapper == null) {
            return false;
        }

        try {
            // {"producerId":"bdc2e83e-5294-451e-a986-a29c7d591d73","score":[{"score":10,"ssrc":196184265}]}
            JSONObject jsonObject = score.getJSONObject(0);
            int scoreNum = jsonObject.optInt("score");
            wrapper.getWrapperCommonLiveData().postValue(value -> {
                wrapper.setProducerScore(scoreNum);
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public ProducersWrapper getProducer(String producerId) {
        return mProducers.get(producerId);
    }

    public void clear() {
        mProducers.clear();
    }
}
