package org.mediasoup.droid.lib.model;

import org.mediasoup.droid.lib.RoomClient;

public class RoomState {

  public enum State{
    Unknown,
    On,
    Off,
    InProgress,
  }

  private RoomClient.ConnectionState connectionState = RoomClient.ConnectionState.NEW;

  private boolean canSendMic;
  private boolean canSendCam;
  private boolean canChangeCam;

  private State cameraState = State.Off;
  private State microphoneState = State.Off;
  /**
   * 前 / 后
   */
  private State cameraSwitchDeviceState = State.On;
  private State restartIceState = State.On;


  public RoomClient.ConnectionState getConnectionState() {
    return connectionState;
  }

  public void setConnectionState(RoomClient.ConnectionState connectionState) {
    this.connectionState = connectionState;
  }

  public boolean isCanSendMic() {
    return canSendMic;
  }

  public void setCanSendMic(boolean canSendMic) {
    this.canSendMic = canSendMic;
  }

  public boolean isCanSendCam() {
    return canSendCam;
  }

  public void setCanSendCam(boolean canSendCam) {
    this.canSendCam = canSendCam;
  }

  public boolean isCanChangeCam() {
    return canChangeCam;
  }

  public void setCanChangeCam(boolean canChangeCam) {
    this.canChangeCam = canChangeCam;
  }

  public State getCameraState() {
    return cameraState;
  }

  public void setCameraState(State cameraState) {
    this.cameraState = cameraState;
  }

  public State getMicrophoneState() {
    return microphoneState;
  }

  public void setMicrophoneState(State microphoneState) {
    this.microphoneState = microphoneState;
  }

  public State getCameraSwitchDeviceState() {
    return cameraSwitchDeviceState;
  }

  public void setCameraSwitchDeviceState(State cameraSwitchDeviceState) {
    this.cameraSwitchDeviceState = cameraSwitchDeviceState;
  }

  public State getRestartIceState() {
    return restartIceState;
  }

  public void setRestartIceState(State restartIceState) {
    this.restartIceState = restartIceState;
  }
}
