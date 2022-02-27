package org.mediasoup.droid.lib.lv;

import org.mediasoup.droid.lib.WrapperCommon;
import org.mediasoup.droid.lib.model.Buddy;
import org.mediasoup.droid.lib.model.RoomState;

public interface BuddyObserver {
    void onBuddyAdd(String id, Buddy buddy);
    void onBuddyRemove(String id);
    void onBuddyVolumeChanged(String id, Buddy buddy);
    void onBuddyStateChanged(String id, Buddy buddy);

    void onProducerAdd(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon);
    void onProducerRemove(String id, Buddy buddy, String producerId);
    void onProducerResumed(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon);
    void onProducerPaused(String id, Buddy buddy, String producerId, WrapperCommon<?> wrapperCommon);
    void onProducerScoreChanged(String id, Buddy buddy, String producerId,WrapperCommon<?> wrapperCommon);

    void onConnectStateChanged(RoomState.State state);
    void onCameraStateChanged(RoomState.State state);
    void onMicrophoneStateChanged(RoomState.State state);
    void onCameraFacingChanged(RoomState.State state);
}
