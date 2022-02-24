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

  private State cameraEnabledState = State.Off;
  private State microphoneEnabledState = State.Off;
  /**
   * 前 / 后
   */
  private State cameraIsFrontDeviceState = State.On;


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

  public State getCameraEnabledState() {
    return cameraEnabledState;
  }

  public void setCameraEnabledState(State cameraEnabledState) {
    this.cameraEnabledState = cameraEnabledState;
  }

  public State getMicrophoneEnabledState() {
    return microphoneEnabledState;
  }

  public void setMicrophoneEnabledState(State microphoneEnabledState) {
    this.microphoneEnabledState = microphoneEnabledState;
  }

  public State getCameraIsFrontDeviceState() {
    return cameraIsFrontDeviceState;
  }

  public void setCameraIsFrontDeviceState(State cameraIsFrontDeviceState) {
    this.cameraIsFrontDeviceState = cameraIsFrontDeviceState;
  }
}
