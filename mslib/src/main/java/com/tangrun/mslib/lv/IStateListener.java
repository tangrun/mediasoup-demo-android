package com.tangrun.mslib.lv;

import com.tangrun.mslib.enums.CameraFacingState;
import com.tangrun.mslib.enums.CameraState;
import com.tangrun.mslib.enums.LocalConnectState;
import com.tangrun.mslib.enums.MicrophoneState;

public interface IStateListener {
    void onLocalConnectStateChanged(LocalConnectState state);
    void onCameraStateChanged(CameraState state);
    void onMicrophoneStateChanged(MicrophoneState state);
    void onCameraFacingChanged(CameraFacingState state);
}
