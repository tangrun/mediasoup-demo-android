package org.mediasoup.droid.lib;

import org.json.JSONArray;
import org.mediasoup.droid.Consumer;
import org.webrtc.MediaStreamTrack;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/15 8:50
 */
public abstract class WrapperCommon {
    protected boolean mLocallyPaused;
    protected boolean mRemotelyPaused;
    protected int mProducerScore = 10;
    protected int mConsumerScore = 10;

    public boolean isLocallyPaused() {
        return mLocallyPaused;
    }

    public boolean isRemotelyPaused() {
        return mRemotelyPaused;
    }

    public int getProducerScore() {
        return mProducerScore;
    }

    public int getConsumerScore() {
        return mConsumerScore;
    }

    public abstract <T extends MediaStreamTrack> T getTrack();
}
