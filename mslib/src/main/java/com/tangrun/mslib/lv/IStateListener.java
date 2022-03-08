package com.tangrun.mslib.lv;

import com.tangrun.mslib.enums.*;

public interface IStateListener {
    void onLocalConnectStateChanged(LocalConnectState state);
    void onCameraStateChanged(CameraState state);
    void onMicrophoneStateChanged(MicrophoneState state);
    void onCameraFacingChanged(CameraFacingState state);
    void onTransportStateChanged(boolean sender,TransportState state);
}
