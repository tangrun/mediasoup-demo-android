package org.mediasoup.droid.lib.lv;

import org.mediasoup.droid.lib.model.WrapperCommon;
import org.mediasoup.droid.lib.model.Buddy;

public interface IProducerListener {
    void onProducerAdd(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon);
    void onProducerRemove(String id, Buddy buddy, String producerId);
    void onProducerResumed(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon);
    void onProducerPaused(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon);
    void onProducerScoreChanged(String id, Buddy buddy, String producerId,WrapperCommon<?> wrapperCommon);
}
