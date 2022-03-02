package com.tangrun.mslib.lv;

import com.tangrun.mslib.model.Buddy;
import com.tangrun.mslib.model.WrapperCommon;

public interface IProducerListener {
    void onProducerAdd(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon);
    void onProducerRemove(String id, Buddy buddy, String producerId);
    void onProducerResumed(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon);
    void onProducerPaused(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon);
    void onProducerScoreChanged(String id, Buddy buddy, String producerId,WrapperCommon<?> wrapperCommon);
}
