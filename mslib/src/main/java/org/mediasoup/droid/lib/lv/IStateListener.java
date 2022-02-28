package org.mediasoup.droid.lib.lv;

import org.mediasoup.droid.lib.enums.CameraFacingState;
import org.mediasoup.droid.lib.enums.CameraState;
import org.mediasoup.droid.lib.enums.LocalConnectState;
import org.mediasoup.droid.lib.enums.MicrophoneState;

public interface IStateListener {
    void onLocalConnectStateChanged(LocalConnectState state);
    void onCameraStateChanged(CameraState state);
    void onMicrophoneStateChanged(MicrophoneState state);
    void onCameraFacingChanged(CameraFacingState state);
}
