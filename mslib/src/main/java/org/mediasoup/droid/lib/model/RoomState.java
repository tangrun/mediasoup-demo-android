package org.mediasoup.droid.lib.model;

import org.mediasoup.droid.lib.RoomClient;

public class RoomState {

  public enum State{
    On,
    Off,
    InProgress,
  }

  private RoomClient.ConnectionState connectionState = RoomClient.ConnectionState.NEW;

  private boolean mCanSendMic;
  private boolean mCanSendCam;
  private boolean mCanChangeCam;

  private State mCam= State.Off;
  private State mMic = State.Off;
  private State mCamSwitch = State.On;
  private State mRestartIce = State.On;

  public RoomClient.ConnectionState getConnectionState() {
    return connectionState;
  }

  public RoomState setConnectionState(RoomClient.ConnectionState connectionState) {
    this.connectionState = connectionState;
    return this;
  }

  public boolean isCanSendMic() {
    return mCanSendMic;
  }

  public RoomState setCanSendMic(boolean mCanSendMic) {
    this.mCanSendMic = mCanSendMic;
    return this;
  }

  public boolean isCanSendCam() {
    return mCanSendCam;
  }

  public RoomState setCanSendCam(boolean mCanSendCam) {
    this.mCanSendCam = mCanSendCam;
    return this;
  }

  public boolean isCanChangeCam() {
    return mCanChangeCam;
  }

  public RoomState setCanChangeCam(boolean mCanChangeCam) {
    this.mCanChangeCam = mCanChangeCam;
    return this;
  }

  public State getCam() {
    return mCam;
  }

  public RoomState setCam(State mCam) {
    this.mCam = mCam;
    return this;
  }

  public State getMic() {
    return mMic;
  }

  public RoomState setMic(State mMic) {
    this.mMic = mMic;
    return this;
  }

  public State getCamSwitch() {
    return mCamSwitch;
  }

  public RoomState setCamSwitch(State mCamSwitch) {
    this.mCamSwitch = mCamSwitch;
    return this;
  }

  public State getRestartIce() {
    return mRestartIce;
  }

  public RoomState setRestartIce(State mRestartIce) {
    this.mRestartIce = mRestartIce;
    return this;
  }
}
