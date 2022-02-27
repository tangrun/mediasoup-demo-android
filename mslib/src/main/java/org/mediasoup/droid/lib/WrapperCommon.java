package org.mediasoup.droid.lib;

import org.mediasoup.droid.Producer;
import org.webrtc.MediaStreamTrack;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/15 8:50
 */
public abstract class WrapperCommon<T> {
    protected String id;
    protected String buddyId;
    protected String kind;
    protected boolean locallyPaused;
    protected boolean remotelyPaused;
    protected Integer producerScore;
    protected Integer consumerScore;
    protected T data;

    public WrapperCommon(String buddyId, String id, String kind, T data) {
        this.buddyId = buddyId;
        this.id = id;
        this.kind = kind;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBuddyId() {
        return buddyId;
    }

    public void setBuddyId(String buddyId) {
        this.buddyId = buddyId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public boolean isLocallyPaused() {
        return locallyPaused;
    }

    public void setLocallyPaused(boolean locallyPaused) {
        this.locallyPaused = locallyPaused;
    }

    public boolean isRemotelyPaused() {
        return remotelyPaused;
    }

    public void setRemotelyPaused(boolean remotelyPaused) {
        this.remotelyPaused = remotelyPaused;
    }

    public Integer getProducerScore() {
        return producerScore;
    }

    public void setProducerScore(Integer producerScore) {
        this.producerScore = producerScore;
    }

    public Integer getConsumerScore() {
        return consumerScore;
    }

    public void setConsumerScore(Integer consumerScore) {
        this.consumerScore = consumerScore;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public abstract <T extends MediaStreamTrack> T getTrack();
    public abstract void close();
}
