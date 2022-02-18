package org.mediasoup.droid.lib;

import androidx.lifecycle.LiveData;

import org.json.JSONArray;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.lib.lv.SupplierMutableLiveData;
import org.webrtc.MediaStreamTrack;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/15 8:50
 */
public abstract class WrapperCommon {
    protected boolean mLocallyPaused;
    protected boolean mRemotelyPaused;
    protected Integer mProducerScore ;
    protected Integer mConsumerScore ;

    public abstract LiveData<WrapperCommon> getWrapperCommonLiveData() ;

    public boolean isLocallyPaused() {
        return mLocallyPaused;
    }

    public boolean isRemotelyPaused() {
        return mRemotelyPaused;
    }

    public Integer getProducerScore() {
        return mProducerScore;
    }

    public Integer getConsumerScore() {
        return mConsumerScore;
    }

    public abstract <T extends MediaStreamTrack> T getTrack();
}
