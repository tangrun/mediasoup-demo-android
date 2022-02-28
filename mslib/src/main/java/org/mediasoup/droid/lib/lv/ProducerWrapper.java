package org.mediasoup.droid.lib.lv;

import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.WrapperCommon;
import org.webrtc.MediaStreamTrack;

public class ProducerWrapper extends WrapperCommon<Producer> {


    public ProducerWrapper(String buddyId, String id, String kind, Producer data) {
        super(buddyId, id, kind, data);
    }

    @Override
    public <T extends MediaStreamTrack> T getTrack() {
        return getData() == null ? null : (T) getData().getTrack();
    }

    @Override
    public void close() {
        if (getData() != null) getData().close();
    }

}
