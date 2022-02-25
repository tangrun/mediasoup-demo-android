package org.mediasoup.droid.lib;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupException;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.mediasoup.droid.lib.model.Buddy;
import org.mediasoup.droid.lib.model.DeviceInfo;
import org.mediasoup.droid.lib.model.RoomState;
import org.mediasoup.droid.lib.socket.WebSocketTransport;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.protoojs.droid.Message;
import org.protoojs.droid.ProtooException;
import org.webrtc.AudioTrack;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.MediaStreamTrack;
import org.webrtc.VideoTrack;

import io.reactivex.disposables.CompositeDisposable;

import static org.mediasoup.droid.lib.JsonUtils.jsonPut;
import static org.mediasoup.droid.lib.JsonUtils.toJsonObject;

public class RoomClient extends RoomMessageHandler {

    public enum ConnectionState {
        // initial state.
        NEW("NEW"),
        // connecting or reconnecting.
        CONNECTING("CONNECTING"),
        RECONNECTING("RECONNECTING"),
        DISCONNECTED("DISCONNECTED"),
        // connected.
        CONNECTED("CONNECTED"),
        // 进入房间
        JOINED("JOINED"),
        // mClosed.
        CLOSED("CLOSED"),
        ;

        ConnectionState(String value) {
            this.value = value;
        }

        String value;

        @Override
        public String toString() {
            return value;
        }
    }

    // Closed flag.
    private volatile boolean mClosed;
    // Android context.
    private final Context mContext;
    // PeerConnection util.
    private PeerConnectionUtils mPeerConnectionUtils;
    // Room mOptions.
    private final RoomOptions mOptions;
    // TODO(Haiyangwu):Next expected dataChannel test number.
    private long mNextDataChannelTestNumber;
    // mProtoo-client Protoo instance.
    private Protoo mProtoo;
    // mediasoup-client Device instance.
    private Device mMediasoupDevice;
    // mediasoup Transport for sending.
    private SendTransport mSendTransport;
    // mediasoup Transport for receiving.
    private RecvTransport mRecvTransport;
    // Local Audio Track for mic.
    private AudioTrack mLocalAudioTrack;
    // Local mic mediasoup Producer.
    private Producer mMicProducer;
    // local Video Track for cam.
    private VideoTrack mLocalVideoTrack;
    // Local cam mediasoup Producer.
    private Producer mCamProducer;
    // TODO(Haiyangwu): Local share mediasoup Producer.
    private Producer mShareProducer;
    // TODO(Haiyangwu): Local chat DataProducer.
    private Producer mChatDataProducer;
    // TODO(Haiyangwu): Local bot DataProducer.
    private Producer mBotDataProducer;
    // jobs worker handler.
    private Handler mWorkHandler;
    // main looper handler.
    private Handler mMainHandler;
    // Disposable Composite. used to cancel running
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public RoomClient(
            @NonNull Context context,
            @NonNull RoomOptions options) {
        super(new RoomStore());
        this.mContext = context.getApplicationContext();
        this.mOptions = options;
        this.mClosed = false;
        this.mStore.addBuddy(new Buddy(true, options.mineId, options.mineDisplayName, options.mineAvatar, DeviceInfo.androidDevice()));
        PeerConnectionUtils.setPreferCameraFace(options.defaultFrontCam ? Constant.camera_front : Constant.camera_rear);
        // init worker handler.
        HandlerThread handlerThread = new HandlerThread("worker");
        handlerThread.start();
        mWorkHandler = new Handler(handlerThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
        mWorkHandler.post(() -> mPeerConnectionUtils = new PeerConnectionUtils());
    }

    @Async
    public void connect() {
        String url = mOptions.getProtooUrl();
        Logger.d(TAG, "connect() " + url);
        mStore.setConnectionState(ConnectionState.CONNECTING);
        mWorkHandler.post(
                () -> {
                    WebSocketTransport transport = new WebSocketTransport(url);
                    mProtoo = new Protoo(transport, peerListener);
                });
    }

    @Async
    public void getPeers() {
        mWorkHandler.post(() -> {
            try {
                String request = mProtoo.syncRequest("getPeers");
                JSONArray jsonArray = JsonUtils.toJsonArray(request);
                mStore.addBuddyForPeers(jsonArray);
            } catch (ProtooException e) {
                e.printStackTrace();
            }
        });
    }

    @Async
    public void hangup() {
        mWorkHandler.post(() -> {
            try {
                mProtoo.syncRequest("hangup");
                close();
            } catch (ProtooException e) {
                e.printStackTrace();
            }
        });
    }

    @Async
    public void addPeers(JSONArray jsonArray) {
        mWorkHandler.post(() -> {
            try {
                mProtoo.syncRequest("addPeers", new Protoo.RequestGenerator() {
                    @Override
                    public void request(JSONObject req) {
                        JsonUtils.jsonPut(req, "peers", jsonArray);
                    }
                });
            } catch (ProtooException e) {
                e.printStackTrace();
            }
        });
    }

    @Async
    public void join() {
        Logger.d(TAG, "join() ");
        mWorkHandler.post(
                () -> {
                    joinImpl();
                });
    }

    @Async
    public void enableMic() {
        if (!mOptions.mProduce || !mOptions.mProduceAudio)
            return;
        Logger.d(TAG, "enableMic()");
        mStore.setMicrophoneState(RoomState.State.InProgress);
        mWorkHandler.post(() -> {
            enableMicImpl();
            mStore.setMicrophoneState(mMicProducer == null ? RoomState.State.Off : RoomState.State.On);
        });
    }

    @Async
    public void disableMic() {
        if (!mOptions.mProduce || !mOptions.mProduceAudio)
            return;
        Logger.d(TAG, "disableMic()");
        mStore.setMicrophoneState(RoomState.State.InProgress);
        mWorkHandler.post(() -> {
            disableMicImpl();
            mStore.setMicrophoneState(mMicProducer == null ? RoomState.State.Off : RoomState.State.On);
        });
    }

    @Async
    public void muteMic() {
        Logger.d(TAG, "muteMic()");
        mWorkHandler.post(this::muteMicImpl);
    }

    @Async
    public void unmuteMic() {
        Logger.d(TAG, "unmuteMic()");
        mWorkHandler.post(this::unmuteMicImpl);
    }

    @Async
    public void enableCam() {
        if (!mOptions.mProduce || !mOptions.mProduceVideo)
            return;
        Logger.d(TAG, "enableCam()");
        mStore.setCameraState(RoomState.State.InProgress);
        mWorkHandler.post(() -> {
            enableCamImpl();
            mStore.setCameraState(mCamProducer == null ? RoomState.State.Off : RoomState.State.On);
        });
    }

    @Async
    public void disableCam() {
        if (!mOptions.mProduce || !mOptions.mProduceVideo)
            return;
        Logger.d(TAG, "disableCam()");
        mStore.setCameraState(RoomState.State.InProgress);
        mWorkHandler.post(() -> {
            disableCamImpl();
            mStore.setCameraState(mCamProducer == null ? RoomState.State.Off : RoomState.State.On);
        });

    }

    @Async
    public void changeCam() {
        if (!mOptions.mProduce || !mOptions.mProduceVideo)
            return;
        Logger.d(TAG, "changeCam()");
        mStore.setCameraSwitchDeviceState(RoomState.State.InProgress);
        mWorkHandler.post(
                () ->
                        mPeerConnectionUtils.switchCam(
                                new CameraVideoCapturer.CameraSwitchHandler() {
                                    @Override
                                    public void onCameraSwitchDone(boolean b) {
                                        mStore.setCameraSwitchDeviceState(b ? RoomState.State.On : RoomState.State.Off);
                                    }

                                    @Override
                                    public void onCameraSwitchError(String s) {
                                        Logger.w(TAG, "changeCam() | failed: " + s);
                                        mStore.addNotify("error", "Could not change cam: " + s);
                                        mStore.setCameraSwitchDeviceState(RoomState.State.Unknown);
                                    }
                                }));
    }

    @Async
    public void disableShare() {
        Logger.d(TAG, "disableShare()");
        // TODO(feature): share
    }

    @Async
    public void enableShare() {
        Logger.d(TAG, "enableShare()");
        // TODO(feature): share
    }

    @Async
    public void restartIceForRecvTransport() {
        mWorkHandler.post(
                () -> {
                    try {
                        if (mRecvTransport != null) {
                            String iceParameters =
                                    mProtoo.syncRequest(
                                            "restartIce", req -> jsonPut(req, "transportId", mRecvTransport.getId()));
                            mRecvTransport.restartIce(iceParameters);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logError("restartIce() | failed:", e);
                        mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                    }
                });
    }

    @Async
    public void restartIceForSendTransport() {
        mWorkHandler.post(
                () -> {
                    try {
                        if (mSendTransport != null) {
                            String iceParameters =
                                    mProtoo.syncRequest(
                                            "restartIce", req -> jsonPut(req, "transportId", mSendTransport.getId()));
                            mSendTransport.restartIce(iceParameters);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logError("restartIce() | failed:", e);
                        mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                    }
                });
    }

    @Async
    public void restartIce() {
        Logger.d(TAG, "restartIce()");
        mWorkHandler.post(
                () -> {
                    try {
                        if (mSendTransport != null) {
                            String iceParameters =
                                    mProtoo.syncRequest(
                                            "restartIce", req -> jsonPut(req, "transportId", mSendTransport.getId()));
                            mSendTransport.restartIce(iceParameters);
                        }
                        if (mRecvTransport != null) {
                            String iceParameters =
                                    mProtoo.syncRequest(
                                            "restartIce", req -> jsonPut(req, "transportId", mRecvTransport.getId()));
                            mRecvTransport.restartIce(iceParameters);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logError("restartIce() | failed:", e);
                        mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                    }
                });
    }

    @Async
    public void setMaxSendingSpatialLayer() {
        Logger.d(TAG, "setMaxSendingSpatialLayer()");
        // TODO(feature): layer
    }

    @Async
    public void setConsumerPreferredLayers(String spatialLayer) {
        Logger.d(TAG, "setConsumerPreferredLayers()");
        // TODO(feature): layer
    }

    @Async
    public void setConsumerPreferredLayers(
            String consumerId, String spatialLayer, String temporalLayer) {
        Logger.d(TAG, "setConsumerPreferredLayers()");
        // TODO: layer
    }

    @Async
    public void requestConsumerKeyFrame(String consumerId) {
        Logger.d(TAG, "requestConsumerKeyFrame()");
        mWorkHandler.post(
                () -> {
                    try {
                        mProtoo.syncRequest(
                                "requestConsumerKeyFrame", req -> jsonPut(req, "consumerId", "consumerId"));
                        mStore.addNotify("Keyframe requested for video consumer");
                    } catch (ProtooException e) {
                        e.printStackTrace();
                        logError("restartIce() | failed:", e);
                        mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                    }
                });
    }

    @Async
    public void enableChatDataProducer() {
        Logger.d(TAG, "enableChatDataProducer()");
        // TODO(feature): data channel
    }

    @Async
    public void enableBotDataProducer() {
        Logger.d(TAG, "enableBotDataProducer()");
        // TODO(feature): data channel
    }

    @Async
    public void sendChatMessage(String txt) {
        Logger.d(TAG, "sendChatMessage()");
        // TODO(feature): data channel
    }

    @Async
    public void sendBotMessage(String txt) {
        Logger.d(TAG, "sendBotMessage()");
        // TODO(feature): data channel
    }

    @Async
    @Deprecated
    public void changeDisplayName(String displayName) {

    }

    @Async
    public void getSendTransportRemoteStats() {
        Logger.d(TAG, "getSendTransportRemoteStats()");
        // TODO(feature): stats
    }

    @Async
    public void getRecvTransportRemoteStats() {
        Logger.d(TAG, "getRecvTransportRemoteStats()");
        // TODO(feature): stats
    }

    @Async
    public void getAudioRemoteStats() {
        Logger.d(TAG, "getAudioRemoteStats()");
        // TODO(feature): stats
    }

    @Async
    public void getVideoRemoteStats() {
        Logger.d(TAG, "getVideoRemoteStats()");
        // TODO(feature): stats
    }

    @Async
    public void getConsumerRemoteStats(String consumerId) {
        Logger.d(TAG, "getConsumerRemoteStats()");
        // TODO(feature): stats
    }

    @Async
    public void getChatDataProducerRemoteStats(String consumerId) {
        Logger.d(TAG, "getChatDataProducerRemoteStats()");
        // TODO(feature): stats
    }

    @Async
    public void getBotDataProducerRemoteStats() {
        Logger.d(TAG, "getBotDataProducerRemoteStats()");
        // TODO(feature): stats
    }

    @Async
    public void getDataConsumerRemoteStats(String dataConsumerId) {
        Logger.d(TAG, "getDataConsumerRemoteStats()");
        // TODO(feature): stats
    }

    @Async
    public void getSendTransportLocalStats() {
        Logger.d(TAG, "getSendTransportLocalStats()");
        // TODO(feature): stats
    }

    @Async
    public void getRecvTransportLocalStats() {
        Logger.d(TAG, "getRecvTransportLocalStats()");
        /// TODO(feature): stats
    }

    @Async
    public void getAudioLocalStats() {
        Logger.d(TAG, "getAudioLocalStats()");
        // TODO(feature): stats
    }

    @Async
    public void getVideoLocalStats() {
        Logger.d(TAG, "getVideoLocalStats()");
        // TODO(feature): stats
    }

    @Async
    public void getConsumerLocalStats(String consumerId) {
        Logger.d(TAG, "getConsumerLocalStats()");
        // TODO(feature): stats
    }

    @Async
    public void applyNetworkThrottle(String uplink, String downlink, String rtt, String secret) {
        Logger.d(TAG, "applyNetworkThrottle()");
        // TODO(feature): stats
    }

    @Async
    public void resetNetworkThrottle(boolean silent, String secret) {
        Logger.d(TAG, "applyNetworkThrottle()");
        // TODO(feature): stats
    }

    @Async
    public void close() {
        if (this.mClosed) {
            return;
        }
        this.mClosed = true;
        Logger.d(TAG, "close()");

        mWorkHandler.post(
                () -> {
                    // Close mProtoo Protoo
                    if (mProtoo != null) {
                        mProtoo.close();
                        mProtoo = null;
                    }

                    // dispose all transport and device.
                    disposeTransportDevice();

                    // dispose audio track.
                    disposeAudioTrack();

                    // dispose video track.
                    disposeVideoTrack();

                    // dispose peerConnection.
                    mPeerConnectionUtils.dispose();

                    // quit worker handler thread.
                    mWorkHandler.getLooper().quit();
                });

        // dispose request.
        mCompositeDisposable.dispose();

        // Set room state.
        mStore.setConnectionState(ConnectionState.CLOSED);
    }

    @WorkerThread
    private void disposeAudioTrack() {
        if (mLocalAudioTrack != null) {
            mLocalAudioTrack.setEnabled(false);
            mLocalAudioTrack.dispose();
            mLocalAudioTrack = null;
        }
        mPeerConnectionUtils.disposeAudioSource();
    }

    @WorkerThread
    private void disposeVideoTrack() {
        if (mLocalVideoTrack != null) {
            mLocalVideoTrack.setEnabled(false);
            mLocalVideoTrack.dispose();
            mLocalVideoTrack = null;
        }
        mPeerConnectionUtils.disposeVideoSource();
    }

    @WorkerThread
    private void disposeTransportDevice() {
        Logger.d(TAG, "disposeTransportDevice()");
        // Close mediasoup Transports.
        if (mSendTransport != null) {
            mSendTransport.close();
            mSendTransport.dispose();
            mSendTransport = null;
        }

        if (mRecvTransport != null) {
            mRecvTransport.close();
            mRecvTransport.dispose();
            mRecvTransport = null;
        }

        // dispose device.
        if (mMediasoupDevice != null) {
            mMediasoupDevice.dispose();
            mMediasoupDevice = null;
        }
    }

    private Protoo.Listener peerListener =
            new Protoo.Listener() {
                @Override
                public void onOpen() {
                    Log.d(TAG, "Protoo.Listener onOpen: ");
                    mWorkHandler.post(() -> {
                        mStore.setConnectionState(ConnectionState.CONNECTED);
                    });
                }

                @Override
                public void onFail() {
                    Log.d(TAG, "Protoo.Listener onFail: ");
                    mWorkHandler.post(
                            () -> {
                                mStore.addNotify("error", "WebSocket connection failed");
                                mStore.setConnectionState(ConnectionState.RECONNECTING);

                                disposeTransportDevice();
                            });
                }

                @Override
                public void onRequest(
                        @NonNull Message.Request request, @NonNull Protoo.ServerRequestHandler handler) {
                    Logger.d(TAG, "Protoo.Listener onRequest() " + request.getMethod() + request.getData().toString());
                    mWorkHandler.post(
                            () -> {
                                try {
                                    switch (request.getMethod()) {
                                        case "newConsumer": {
                                            onNewConsumer(request, handler);
                                            break;
                                        }
                                        case "newDataConsumer": {
                                            onNewDataConsumer(request, handler);
                                            break;
                                        }
                                        default: {
                                            handler.reject(403, "unknown protoo request.method " + request.getMethod());
                                            Logger.w(TAG, "unknown protoo request.method " + request.getMethod());
                                        }
                                    }
                                } catch (Exception e) {
                                    Logger.e(TAG, "handleRequestError.", e);
                                }
                            });
                }

                @Override
                public void onNotification(@NonNull Message.Notification notification) {
                    Logger.d(
                            TAG,
                            "Protoo.Listener onNotification() "
                                    + notification.getMethod()
                                    + ", "
                                    + notification.getData().toString());
                    mWorkHandler.post(
                            () -> {
                                try {
                                    handleNotification(notification);
                                } catch (Exception e) {
                                    Logger.e(TAG, "handleNotification error.", e);
                                }
                            });
                }

                @Override
                public void onDisconnected() {
                    Log.d(TAG, "Protoo.Listener onDisconnected: ");
                    mWorkHandler.post(
                            () -> {
                                mStore.addNotify("error", "WebSocket disconnected");
                                mStore.setConnectionState(ConnectionState.RECONNECTING);

                                // Close All Transports created by device.
                                // All will reCreated After ReJoin.
                                disposeTransportDevice();
                            });
                }

                @Override
                public void onClose() {
                    Log.d(TAG, "Protoo.Listener onClose: " + mClosed);
                    mWorkHandler.post(
                            () -> {
                                close();
                            });
                }
            };

    @WorkerThread
    private void joinImpl() {
        Logger.d(TAG, "joinImpl()");

        try {

            mMediasoupDevice = new Device();
            String routerRtpCapabilities = mProtoo.syncRequest("getRouterRtpCapabilities");
            mMediasoupDevice.load(routerRtpCapabilities);

            // Create mediasoup Transport for sending (unless we don't want to produce).
            if (mOptions.mProduce) {
                createSendTransport();
            }

            // Create mediasoup Transport for sending (unless we don't want to consume).
            if (mOptions.mConsume) {
                createRecvTransport();
            }

            String rtpCapabilities = mMediasoupDevice.getRtpCapabilities();
            // Join now into the room.
            // TODO(HaiyangWu): Don't send our RTP capabilities if we don't want to consume.
            String joinResponse =
                    mProtoo.syncRequest(
                            "join",
                            req -> {
                                //jsonPut(req, "displayName", mOptions.me.getDisplayName());
//                                jsonPut(req, "device", DeviceInfo.androidDevice().toJSONObject());
                                //jsonPut(req, "avatar", mOptions.me.getAvatar());
                                jsonPut(req, "rtpCapabilities", toJsonObject(rtpCapabilities));
                                // TODO (HaiyangWu): add sctpCapabilities
                                jsonPut(req, "sctpCapabilities", "");
                            });

            mStore.setConnectionState(ConnectionState.JOINED);

            JSONObject resObj = JsonUtils.toJsonObject(joinResponse);
            JSONArray peers = resObj.optJSONArray("peers");
            mStore.addBuddyForPeers(peers);
//            for (int i = 0; peers != null && i < peers.length(); i++) {
//                JSONObject peer = peers.getJSONObject(i);
//                mStore.addBuddyForPeer(peer.optString("id"), peer);
//            }

            // Enable mic/webcam.
            // 取消自动上传流 在界面判断
//      if (mOptions.mProduce) {
//        boolean canSendMic = mMediasoupDevice.canProduce("audio");
//        boolean canSendCam = mMediasoupDevice.canProduce("video");
//        mStore.setMediaCapabilities(canSendMic, canSendCam);
//        mMainHandler.post(this::enableMic);
//        mMainHandler.post(this::enableCam);
//      }
        } catch (Exception e) {
            e.printStackTrace();
            logError("joinRoom() failed:", e);
            if (TextUtils.isEmpty(e.getMessage())) {
                mStore.addNotify("error", "Could not join the room, internal error");
            } else {
                mStore.addNotify("error", "Could not join the room: " + e.getMessage());
            }
            mMainHandler.post(this::close);
        }
    }

    @WorkerThread
    private void enableMicImpl() {
        Logger.d(TAG, "enableMicImpl()");
        try {
            if (mMicProducer != null || mMediasoupDevice ==null) {
                return;
            }
            if (!mMediasoupDevice.isLoaded()) {
                Logger.w(TAG, "enableMic() | not loaded");
                return;
            }
            if (!mMediasoupDevice.canProduce(Constant.kind_audio)) {
                Logger.w(TAG, "enableMic() | cannot produce audio");
                return;
            }
            if (mSendTransport == null) {
                Logger.w(TAG, "enableMic() | mSendTransport doesn't ready");
                return;
            }
            if (mLocalAudioTrack == null || mLocalAudioTrack.state() == MediaStreamTrack.State.ENDED) {
                mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(mContext, "mic");
                mLocalAudioTrack.setEnabled(true);
            }
            mMicProducer =
                    mSendTransport.produce(
                            producer -> {
                                Logger.e(TAG, "onTransportClose(), micProducer");
                                if (mMicProducer != null) {
                                    mStore.removeProducer(mOptions.mineId, mMicProducer.getId());
                                    mMicProducer = null;
                                }
                            },
                            mLocalAudioTrack,
                            null,
                            null);
            mStore.addProducer(mOptions.mineId, mMicProducer);
        } catch (MediasoupException e) {
            e.printStackTrace();
            logError("enableMic() | failed:", e);
            mStore.addNotify("error", "Error enabling microphone: " + e.getMessage());
            if (mLocalAudioTrack != null) {
                mLocalAudioTrack.setEnabled(false);
            }
        }
    }

    @WorkerThread
    private void disableMicImpl() {
        Logger.d(TAG, "disableMicImpl()");
        if (mMicProducer == null) {
            return;
        }

        mMicProducer.close();
        mStore.removeProducer(mOptions.mineId, mMicProducer.getId());

        try {
            mProtoo.syncRequest("closeProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
        } catch (ProtooException e) {
            e.printStackTrace();
            mStore.addNotify("error", "Error closing server-side mic Producer: " + e.getMessage());
        }

        disposeAudioTrack();
        mMicProducer = null;
    }

    @Deprecated
    @WorkerThread
    private void muteMicImpl() {

    }

    @Deprecated
    @WorkerThread
    private void unmuteMicImpl() {

    }

    @WorkerThread
    private void enableCamImpl() {
        Logger.d(TAG, "enableCamImpl()");
        try {
            if (mCamProducer != null || mMediasoupDevice ==null) {
                return;
            }
            if (!mMediasoupDevice.isLoaded()) {
                Logger.w(TAG, "enableCam() | not loaded");
                return;
            }
            if (!mMediasoupDevice.canProduce(Constant.kind_video)) {
                Logger.w(TAG, "enableCam() | cannot produce video");
                return;
            }
            if (mSendTransport == null) {
                Logger.w(TAG, "enableCam() | mSendTransport doesn't ready");
                return;
            }

            if (mLocalVideoTrack == null || mLocalVideoTrack.state() == MediaStreamTrack.State.ENDED) {
                mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext, "cam");
                mLocalVideoTrack.setEnabled(true);
            }
            mCamProducer =
                    mSendTransport.produce(
                            producer -> {
                                Logger.e(TAG, "onTransportClose(), camProducer");
                                if (mCamProducer != null) {
                                    mStore.removeProducer(mOptions.mineId, mCamProducer.getId());
                                    mCamProducer = null;
                                }
                            },
                            mLocalVideoTrack,
                            null,
                            null);
            mStore.addProducer(mOptions.mineId, mCamProducer);
        } catch (MediasoupException e) {
            e.printStackTrace();
            logError("enableWebcam() | failed:", e);
            mStore.addNotify("error", "Error enabling webcam: " + e.getMessage());
            if (mLocalVideoTrack != null) {
                mLocalVideoTrack.setEnabled(false);
            }
        }
    }

    @WorkerThread
    private void disableCamImpl() {
        Logger.d(TAG, "disableCamImpl()");
        if (mCamProducer == null) {
            return;
        }

        mCamProducer.close();
        mStore.removeProducer(mOptions.mineId, mCamProducer.getId());

        try {
            mProtoo.syncRequest("closeProducer", req -> jsonPut(req, "producerId", mCamProducer.getId()));
        } catch (ProtooException e) {
            e.printStackTrace();
            mStore.addNotify("error", "Error closing server-side webcam Producer: " + e.getMessage());
        }

        disposeVideoTrack();
        mCamProducer = null;
    }

    @WorkerThread
    private void createSendTransport() throws ProtooException, JSONException, MediasoupException {
        Logger.d(TAG, "createSendTransport()");
        String res =
                mProtoo.syncRequest(
                        "createWebRtcTransport",
                        (req -> {
                            jsonPut(req, "forceTcp", mOptions.mForceTcp);
                            jsonPut(req, "producing", true);
                            jsonPut(req, "consuming", false);
                            // TODO: sctpCapabilities
                            jsonPut(req, "sctpCapabilities", "");
                        }));
        JSONObject info = new JSONObject(res);

        Logger.d(TAG, "device#createSendTransport() " + info);
        String id = info.optString("id");
        String iceParameters = info.optString("iceParameters");
        String iceCandidates = info.optString("iceCandidates");
        String dtlsParameters = info.optString("dtlsParameters");
        String sctpParameters = info.optString("sctpParameters");

        mSendTransport =
                mMediasoupDevice.createSendTransport(
                        sendTransportListener, id, iceParameters, iceCandidates, dtlsParameters);
    }

    @WorkerThread
    private void createRecvTransport() throws ProtooException, JSONException, MediasoupException {
        Logger.d(TAG, "createRecvTransport()");

        String res =
                mProtoo.syncRequest(
                        "createWebRtcTransport",
                        req -> {
                            jsonPut(req, "forceTcp", mOptions.mForceTcp);
                            jsonPut(req, "producing", false);
                            jsonPut(req, "consuming", true);
                            // TODO (HaiyangWu): add sctpCapabilities
                            jsonPut(req, "sctpCapabilities", "");
                        });
        JSONObject info = new JSONObject(res);
        Logger.d(TAG, "device#createRecvTransport() " + info);
        String id = info.optString("id");
        String iceParameters = info.optString("iceParameters");
        String iceCandidates = info.optString("iceCandidates");
        String dtlsParameters = info.optString("dtlsParameters");
        String sctpParameters = info.optString("sctpParameters");

        mRecvTransport =
                mMediasoupDevice.createRecvTransport(
                        recvTransportListener, id, iceParameters, iceCandidates, dtlsParameters, null);
    }

    private SendTransport.Listener sendTransportListener =
            new SendTransport.Listener() {

                private String listenerTAG = TAG + "_SendTrans";

                @Override
                public String onProduce(
                        Transport transport, String kind, String rtpParameters, String appData) {
                    if (mClosed) {
                        return "";
                    }
                    Logger.d(listenerTAG, "onProduce() ");
                    String producerId =
                            fetchProduceId(
                                    req -> {
                                        jsonPut(req, "transportId", transport.getId());
                                        jsonPut(req, "kind", kind);
                                        jsonPut(req, "rtpParameters", toJsonObject(rtpParameters));
                                        jsonPut(req, "appData", appData);
                                    });
                    Logger.d(listenerTAG, "producerId: " + producerId);
                    return producerId;
                }

                @Override
                public void onConnect(Transport transport, String dtlsParameters) {
                    if (mClosed) {
                        return;
                    }
                    Logger.d(listenerTAG + "_send", "onConnect()");
                    mCompositeDisposable.add(
                            mProtoo
                                    .request(
                                            "connectWebRtcTransport",
                                            req -> {
                                                jsonPut(req, "transportId", transport.getId());
                                                jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters));
                                            })
                                    .subscribe(
                                            d -> Logger.d(listenerTAG, "connectWebRtcTransport res: " + d),
                                            t -> logError("connectWebRtcTransport for mSendTransport failed", t)));
                }

                @Override
                public void onConnectionStateChange(Transport transport, String connectionState) {
                    // connected completed disconnected failed
                    Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState);
                    if ("failed".equals(connectionState)){
                        restartIceForSendTransport();
                    }
                }
            };

    private RecvTransport.Listener recvTransportListener =
            new RecvTransport.Listener() {

                private String listenerTAG = TAG + "_RecvTrans";

                @Override
                public void onConnect(Transport transport, String dtlsParameters) {
                    if (mClosed) {
                        return;
                    }
                    Logger.d(listenerTAG, "onConnect()");
                    mCompositeDisposable.add(
                            mProtoo
                                    .request(
                                            "connectWebRtcTransport",
                                            req -> {
                                                jsonPut(req, "transportId", transport.getId());
                                                jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters));
                                            })
                                    .subscribe(
                                            d -> Logger.d(listenerTAG, "connectWebRtcTransport res: " + d),
                                            t -> logError("connectWebRtcTransport for mRecvTransport failed", t)));
                }

                @Override
                public void onConnectionStateChange(Transport transport, String connectionState) {
                    Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState);
                    if ("failed".equals(connectionState)){
                        restartIceForRecvTransport();
                    }
                }
            };

    private String fetchProduceId(Protoo.RequestGenerator generator) {
        Logger.d(TAG, "fetchProduceId:()");
        try {
            String response = mProtoo.syncRequest("produce", generator);
            return new JSONObject(response).optString("id");
        } catch (ProtooException | JSONException e) {
            e.printStackTrace();
            logError("send produce request failed", e);
            return "";
        }
    }

    private void logError(String message, Throwable throwable) {
        Logger.e(TAG, message, throwable);
    }

    private void onNewConsumer(Message.Request request, Protoo.ServerRequestHandler handler) {
        if (!mOptions.mConsume) {
            handler.reject(403, "I do not want to consume");
            return;
        }
        try {
            JSONObject data = request.getData();
            String peerId = data.optString("peerId");
            String producerId = data.optString("producerId");
            String id = data.optString("id");
            String kind = data.optString("kind");
            String rtpParameters = data.optString("rtpParameters");
            String type = data.optString("type");
            String appData = data.optString("appData");
            boolean producerPaused = data.optBoolean("producerPaused");

            if ((Constant.kind_audio.equals(kind) && !mOptions.mConsumeAudio)
                    || Constant.kind_video.equals(kind) && !mOptions.mConsumeVideo
            ) {
                handler.reject(403, "I do not want to consume");
                return;
            }

            Consumer consumer =
                    mRecvTransport.consume(
                            c -> {
                                mConsumers.remove(c.getId());
                                Logger.w(TAG, "onTransportClose for consume");
                            },
                            id,
                            producerId,
                            kind,
                            rtpParameters,
                            appData);

            mConsumers.put(consumer.getId(), new ConsumerHolder(peerId, consumer));
            mStore.addConsumer(peerId, type, consumer, producerPaused);

            // We are ready. Answer the protoo request so the server will
            // resume this Consumer (which was paused for now if video).
            handler.accept();
        } catch (Exception e) {
            e.printStackTrace();
            logError("\"newConsumer\" request failed:", e);
            mStore.addNotify("error", "Error creating a Consumer: " + e.getMessage());
        }
    }

    private void onNewDataConsumer(Message.Request request, Protoo.ServerRequestHandler handler) {
        handler.reject(403, "I do not want to data consume");
        // TODO(HaiyangWu): support data consume
    }

    @Deprecated
    @WorkerThread
    private void pauseConsumer(Consumer consumer) {

    }

    @Deprecated
    @WorkerThread
    private void resumeConsumer(Consumer consumer) {

    }

    public RoomStore getStore() {
        return mStore;
    }

    public RoomOptions getOptions() {
        return mOptions;
    }
}
