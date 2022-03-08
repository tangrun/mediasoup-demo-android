package com.tangrun.mslib;

import android.content.Context;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import com.tangrun.mslib.enums.*;
import com.tangrun.mslib.model.Buddy;
import com.tangrun.mslib.model.DeviceInfo;
import com.tangrun.mslib.socket.Protoo;
import com.tangrun.mslib.socket.WebSocketTransport;
import com.tangrun.mslib.utils.ArchTaskExecutor;
import com.tangrun.mslib.utils.JsonUtils;
import com.tangrun.mslib.utils.PeerConnectionUtils;
import io.reactivex.disposables.CompositeDisposable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.*;
import org.protoojs.droid.Message;
import org.protoojs.droid.ProtooException;
import org.webrtc.AudioTrack;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.MediaStreamTrack;
import org.webrtc.VideoTrack;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RoomClient extends RoomMessageHandler {


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
    private Executor mWorkHandler;
    // main looper handler.
    private Executor mMainHandler;
    // Disposable Composite. used to cancel running
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private boolean canSendMic;
    private boolean canSendCam;
    private boolean canChangeCam;

    public RoomClient(
            @NonNull Context context,
            @NonNull RoomOptions options) {
        super(new RoomStore());
        this.mContext = context.getApplicationContext();
        this.mOptions = options;
        this.mClosed = false;
        this.mStore.addBuddy(new Buddy(true, options.mineId, options.mineDisplayName, options.mineAvatar, DeviceInfo.androidDevice()));
        PeerConnectionUtils.setPreferCameraFace(options.defaultFrontCam ? FrontFacing.front.value : FrontFacing.rear.value);
        // init worker handler.
        HandlerThread handlerThread = new HandlerThread("worker");
        handlerThread.start();
        mWorkHandler = Executors.newSingleThreadExecutor();
        mMainHandler = ArchTaskExecutor.getMainThreadExecutor();
        mWorkHandler.execute(() -> mPeerConnectionUtils = new PeerConnectionUtils());
    }


    public void connect(JSONArray users) {
        String url = mOptions.getProtooUrl(users);
        Logger.d(TAG, "connect() " + url);
        mStore.setLocalConnectionState(LocalConnectState.CONNECTING);
        mWorkHandler.execute(
                () -> {
                    WebSocketTransport transport = new WebSocketTransport(url);
                    mProtoo = new Protoo(transport, peerListener);
                });
    }


    public void getPeer(String peerId, androidx.core.util.Consumer<Buddy> consumer) {
        if (consumer == null || peerId == null) return;
        mWorkHandler.execute(() -> {
            try {
                String resp = mProtoo.syncRequest("getPeer", req -> {
                    try {
                        req.put("peerId", peerId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
                if (resp != null && !resp.trim().isEmpty()) {
                    consumer.accept(new Buddy(false, JsonUtils.toJsonObject(resp)));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            consumer.accept(null);
        });


    }


    public void hangup() {
        mWorkHandler.execute(() -> {
            try {
                mProtoo.syncRequest("hangup");
                close();
            } catch (ProtooException e) {
                e.printStackTrace();
            }
        });
    }

    public void addPeers(JSONArray jsonArray) {
        mWorkHandler.execute(() -> {
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


    public void join() {
        Logger.d(TAG, "join() ");
        mWorkHandler.execute(
                () -> {
                    joinImpl();
                });
    }


    public void enableMic() {
        if (!mOptions.mProduce || !mOptions.mProduceAudio)
            return;
        Logger.d(TAG, "enableMic()");
        mStore.setMicrophoneState(MicrophoneState.inProgress);
        mWorkHandler.execute(() -> {
            enableMicImpl();
            mStore.setMicrophoneState(mMicProducer == null ? MicrophoneState.disabled : MicrophoneState.enabled);
        });
    }


    public void disableMic() {
        if (!mOptions.mProduce || !mOptions.mProduceAudio)
            return;
        Logger.d(TAG, "disableMic()");
        mStore.setMicrophoneState(MicrophoneState.inProgress);
        mWorkHandler.execute(() -> {
            disableMicImpl();
            mStore.setMicrophoneState(mMicProducer == null ? MicrophoneState.disabled : MicrophoneState.enabled);
        });
    }


    public void muteMic() {
        Logger.d(TAG, "muteMic()");
        mWorkHandler.execute(this::muteMicImpl);
    }


    public void unmuteMic() {
        Logger.d(TAG, "unmuteMic()");
        mWorkHandler.execute(this::unmuteMicImpl);
    }


    public void enableCam() {
        if (!mOptions.mProduce || !mOptions.mProduceVideo)
            return;
        Logger.d(TAG, "enableCam()");
        mStore.setCameraState(CameraState.inProgress);
        mWorkHandler.execute(() -> {
            enableCamImpl();
            mStore.setCameraState(mCamProducer == null ? CameraState.disabled : CameraState.enabled);
        });
    }


    public void disableCam() {
        if (!mOptions.mProduce || !mOptions.mProduceVideo)
            return;
        Logger.d(TAG, "disableCam()");
        mStore.setCameraState(CameraState.inProgress);
        mWorkHandler.execute(() -> {
            disableCamImpl();
            mStore.setCameraState(mCamProducer == null ? CameraState.disabled : CameraState.enabled);
        });

    }


    public void changeCam() {
        if (!mOptions.mProduce || !mOptions.mProduceVideo)
            return;
        if (!canChangeCam) {
            return;
        }
        Logger.d(TAG, "changeCam()");
        CameraFacingState oldCameraFacingState = mStore.getCameraFacingState();
        mStore.setCameraFacingState(CameraFacingState.inProgress);
        mWorkHandler.execute(
                () ->
                        mPeerConnectionUtils.switchCam(
                                new CameraVideoCapturer.CameraSwitchHandler() {
                                    @Override
                                    public void onCameraSwitchDone(boolean b) {
                                        mStore.setCameraFacingState(b ? CameraFacingState.front : CameraFacingState.rear);
                                    }

                                    @Override
                                    public void onCameraSwitchError(String s) {
                                        Logger.w(TAG, "changeCam() | failed: " + s);
                                        mStore.addNotify("error", "Could not change cam: " + s);
                                        mStore.setCameraFacingState(oldCameraFacingState);
                                    }
                                }));
    }


    public void disableShare() {
        Logger.d(TAG, "disableShare()");
        // TODO(feature): share
    }


    public void enableShare() {
        Logger.d(TAG, "enableShare()");
        // TODO(feature): share
    }


    public void restartIceForRecvTransport() {
        mWorkHandler.execute(
                () -> {
                    try {
                        if (mRecvTransport != null) {
                            String iceParameters =
                                    mProtoo.syncRequest(
                                            "restartIce", req -> JsonUtils.jsonPut(req, "transportId", mRecvTransport.getId()));
                            mRecvTransport.restartIce(iceParameters);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logError("restartIce() | failed:", e);
                        mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                    }
                });
    }


    public void restartIceForSendTransport() {
        mWorkHandler.execute(
                () -> {
                    try {
                        if (mSendTransport != null) {
                            String iceParameters =
                                    mProtoo.syncRequest(
                                            "restartIce", req -> JsonUtils.jsonPut(req, "transportId", mSendTransport.getId()));
                            mSendTransport.restartIce(iceParameters);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logError("restartIce() | failed:", e);
                        mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                    }
                });
    }


    public void restartIce() {
        Logger.d(TAG, "restartIce()");
        mWorkHandler.execute(
                () -> {
                    try {
                        if (mSendTransport != null) {
                            String iceParameters =
                                    mProtoo.syncRequest(
                                            "restartIce", req -> JsonUtils.jsonPut(req, "transportId", mSendTransport.getId()));
                            mSendTransport.restartIce(iceParameters);
                        }
                        if (mRecvTransport != null) {
                            String iceParameters =
                                    mProtoo.syncRequest(
                                            "restartIce", req -> JsonUtils.jsonPut(req, "transportId", mRecvTransport.getId()));
                            mRecvTransport.restartIce(iceParameters);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logError("restartIce() | failed:", e);
                        mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                    }
                });
    }


    public void setMaxSendingSpatialLayer() {
        Logger.d(TAG, "setMaxSendingSpatialLayer()");
        // TODO(feature): layer
    }


    public void setConsumerPreferredLayers(String spatialLayer) {
        Logger.d(TAG, "setConsumerPreferredLayers()");
        // TODO(feature): layer
    }


    public void setConsumerPreferredLayers(
            String consumerId, String spatialLayer, String temporalLayer) {
        Logger.d(TAG, "setConsumerPreferredLayers()");
        // TODO: layer
    }


    public void requestConsumerKeyFrame(String consumerId) {
        Logger.d(TAG, "requestConsumerKeyFrame()");
        mWorkHandler.execute(
                () -> {
                    try {
                        mProtoo.syncRequest(
                                "requestConsumerKeyFrame", req -> JsonUtils.jsonPut(req, "consumerId", "consumerId"));
                        mStore.addNotify("Keyframe requested for video consumer");
                    } catch (ProtooException e) {
                        e.printStackTrace();
                        logError("restartIce() | failed:", e);
                        mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                    }
                });
    }


    public void enableChatDataProducer() {
        Logger.d(TAG, "enableChatDataProducer()");
        // TODO(feature): data channel
    }


    public void enableBotDataProducer() {
        Logger.d(TAG, "enableBotDataProducer()");
        // TODO(feature): data channel
    }


    public void sendChatMessage(String txt) {
        Logger.d(TAG, "sendChatMessage()");
        // TODO(feature): data channel
    }


    public void sendBotMessage(String txt) {
        Logger.d(TAG, "sendBotMessage()");
        // TODO(feature): data channel
    }


    @Deprecated
    public void changeDisplayName(String displayName) {

    }


    public void getSendTransportRemoteStats() {
        Logger.d(TAG, "getSendTransportRemoteStats()");
        // TODO(feature): stats
    }


    public void getRecvTransportRemoteStats() {
        Logger.d(TAG, "getRecvTransportRemoteStats()");
        // TODO(feature): stats
    }


    public void getAudioRemoteStats() {
        Logger.d(TAG, "getAudioRemoteStats()");
        // TODO(feature): stats
    }


    public void getVideoRemoteStats() {
        Logger.d(TAG, "getVideoRemoteStats()");
        // TODO(feature): stats
    }


    public void getConsumerRemoteStats(String consumerId) {
        Logger.d(TAG, "getConsumerRemoteStats()");
        // TODO(feature): stats
    }


    public void getChatDataProducerRemoteStats(String consumerId) {
        Logger.d(TAG, "getChatDataProducerRemoteStats()");
        // TODO(feature): stats
    }


    public void getBotDataProducerRemoteStats() {
        Logger.d(TAG, "getBotDataProducerRemoteStats()");
        // TODO(feature): stats
    }


    public void getDataConsumerRemoteStats(String dataConsumerId) {
        Logger.d(TAG, "getDataConsumerRemoteStats()");
        // TODO(feature): stats
    }


    public void getSendTransportLocalStats() {
        Logger.d(TAG, "getSendTransportLocalStats()");
        // TODO(feature): stats
    }


    public void getRecvTransportLocalStats() {
        Logger.d(TAG, "getRecvTransportLocalStats()");
        /// TODO(feature): stats
    }


    public void getAudioLocalStats() {
        Logger.d(TAG, "getAudioLocalStats()");
        // TODO(feature): stats
    }


    public void getVideoLocalStats() {
        Logger.d(TAG, "getVideoLocalStats()");
        // TODO(feature): stats
    }


    public void getConsumerLocalStats(String consumerId) {
        Logger.d(TAG, "getConsumerLocalStats()");
        // TODO(feature): stats
    }


    public void applyNetworkThrottle(String uplink, String downlink, String rtt, String secret) {
        Logger.d(TAG, "applyNetworkThrottle()");
        // TODO(feature): stats
    }


    public void resetNetworkThrottle(boolean silent, String secret) {
        Logger.d(TAG, "applyNetworkThrottle()");
        // TODO(feature): stats
    }


    public void close() {
        if (this.mClosed) {
            return;
        }
        this.mClosed = true;
        Logger.d(TAG, "close()");

        mWorkHandler.execute(
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
//                    mWorkHandler.getLooper().quit();
                });

        // dispose request.
        mCompositeDisposable.dispose();

        // Set room state.
        mStore.setLocalConnectionState(LocalConnectState.CLOSED);
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
            mStore.getClientObservable().getDispatcher().onTransportStateChanged(true,TransportState.disposed);
            mSendTransport.close();
            mSendTransport.dispose();
            mSendTransport = null;
        }

        if (mRecvTransport != null) {
            mStore.getClientObservable().getDispatcher().onTransportStateChanged(false,TransportState.disposed);
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
                    mWorkHandler.execute(() -> {
                        mStore.setLocalConnectionState(LocalConnectState.CONNECTED);
                    });
                }

                @Override
                public void onFail() {
                    Log.d(TAG, "Protoo.Listener onFail: ");
                    mWorkHandler.execute(
                            () -> {
                                mStore.addNotify("error", "WebSocket connection failed");
                                mStore.setLocalConnectionState(LocalConnectState.RECONNECTING);

                                disposeTransportDevice();
                            });
                }

                @Override
                public void onRequest(
                        @NonNull Message.Request request, @NonNull Protoo.ServerRequestHandler handler) {
                    Logger.d(TAG, "Protoo.Listener onRequest() " + request.getMethod() + request.getData().toString());
                    mWorkHandler.execute(
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
                    mWorkHandler.execute(
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
                    mWorkHandler.execute(
                            () -> {
                                mStore.addNotify("error", "WebSocket disconnected");
                                mStore.setLocalConnectionState(LocalConnectState.RECONNECTING);

                                // Close All Transports created by device.
                                // All will reCreated After ReJoin.
                                disposeTransportDevice();
                            });
                }

                @Override
                public void onClose() {
                    Log.d(TAG, "Protoo.Listener onClose: " + mClosed);
                    mWorkHandler.execute(
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

            canSendMic = mMediasoupDevice.canProduce(Kind.audio.value);
            canSendCam = mMediasoupDevice.canProduce(Kind.video.value);
            canChangeCam = mPeerConnectionUtils.canChangeCam(mContext);

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
                                JsonUtils.jsonPut(req, "rtpCapabilities", JsonUtils.toJsonObject(rtpCapabilities));
                                // TODO (HaiyangWu): add sctpCapabilities
                                JsonUtils.jsonPut(req, "sctpCapabilities", "");
                            });

            mStore.setLocalConnectionState(LocalConnectState.JOINED);

            JSONObject resObj = JsonUtils.toJsonObject(joinResponse);
            JSONArray peers = resObj.optJSONArray("peers");
            mStore.addBuddyForPeers(peers);


        } catch (Exception e) {
            e.printStackTrace();
            logError("joinRoom() failed:", e);
            if (TextUtils.isEmpty(e.getMessage())) {
                mStore.addNotify("error", "Could not join the room, internal error");
            } else {
                mStore.addNotify("error", "Could not join the room: " + e.getMessage());
            }
            mMainHandler.execute(this::close);
        }
    }

    @WorkerThread
    private void enableMicImpl() {
        Logger.d(TAG, "enableMicImpl()");
        try {
            if (mMicProducer != null || mMediasoupDevice == null) {
                return;
            }
            if (!mMediasoupDevice.isLoaded()) {
                Logger.w(TAG, "enableMic() | not loaded");
                return;
            }
            if (!canSendMic) {
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
                                    mStore.removeWrapper(false, mMicProducer.getId());
                                    mMicProducer = null;
                                }
                            },
                            mLocalAudioTrack,
                            null,
                            null);
            mStore.addWrapper(true, mOptions.mineId, mMicProducer.getId(), mMicProducer.getKind(), mMicProducer);
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

        mStore.removeWrapper(true, mMicProducer.getId());

        try {
            mProtoo.syncRequest("closeProducer", req -> JsonUtils.jsonPut(req, "producerId", mMicProducer.getId()));
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
            if (mCamProducer != null || mMediasoupDevice == null) {
                return;
            }
            if (!mMediasoupDevice.isLoaded()) {
                Logger.w(TAG, "enableCam() | not loaded");
                return;
            }
            if (!canSendCam) {
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
                                    mStore.removeWrapper(false, mCamProducer.getId());
                                    mCamProducer = null;
                                }
                            },
                            mLocalVideoTrack,
                            null,
                            null);
            mStore.addWrapper(true, mOptions.mineId, mCamProducer.getId(), mCamProducer.getKind(), mCamProducer);
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

        mStore.removeWrapper(true, mCamProducer.getId());

        try {
            mProtoo.syncRequest("closeProducer", req -> JsonUtils.jsonPut(req, "producerId", mCamProducer.getId()));
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
                            JsonUtils.jsonPut(req, "forceTcp", mOptions.mForceTcp);
                            JsonUtils.jsonPut(req, "producing", true);
                            JsonUtils.jsonPut(req, "consuming", false);
                            // TODO: sctpCapabilities
                            JsonUtils.jsonPut(req, "sctpCapabilities", "");
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
        mStore.getClientObservable().getDispatcher().onTransportStateChanged(true, TransportState.created);
    }

    @WorkerThread
    private void createRecvTransport() throws ProtooException, JSONException, MediasoupException {
        Logger.d(TAG, "createRecvTransport()");

        String res =
                mProtoo.syncRequest(
                        "createWebRtcTransport",
                        req -> {
                            JsonUtils.jsonPut(req, "forceTcp", mOptions.mForceTcp);
                            JsonUtils.jsonPut(req, "producing", false);
                            JsonUtils.jsonPut(req, "consuming", true);
                            // TODO (HaiyangWu): add sctpCapabilities
                            JsonUtils.jsonPut(req, "sctpCapabilities", "");
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
        mStore.getClientObservable().getDispatcher().onTransportStateChanged(false, TransportState.created);
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
                                        JsonUtils.jsonPut(req, "transportId", transport.getId());
                                        JsonUtils.jsonPut(req, "kind", kind);
                                        JsonUtils.jsonPut(req, "rtpParameters", JsonUtils.toJsonObject(rtpParameters));
                                        JsonUtils.jsonPut(req, "appData", appData);
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
                                                JsonUtils.jsonPut(req, "transportId", transport.getId());
                                                JsonUtils.jsonPut(req, "dtlsParameters", JsonUtils.toJsonObject(dtlsParameters));
                                            })
                                    .subscribe(
                                            d -> Logger.d(listenerTAG, "connectWebRtcTransport res: " + d),
                                            t -> logError("connectWebRtcTransport for mSendTransport failed", t)));
                }

                @Override
                public void onConnectionStateChange(Transport transport, String connectionState) {
                    // connected completed disconnected failed
                    Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState);
                    if ("failed".equals(connectionState)) {
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
                                                JsonUtils.jsonPut(req, "transportId", transport.getId());
                                                JsonUtils.jsonPut(req, "dtlsParameters", JsonUtils.toJsonObject(dtlsParameters));
                                            })
                                    .subscribe(
                                            d -> Logger.d(listenerTAG, "connectWebRtcTransport res: " + d),
                                            t -> logError("connectWebRtcTransport for mRecvTransport failed", t)));
                }

                @Override
                public void onConnectionStateChange(Transport transport, String connectionState) {
                    Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState);
                    // disconnected closed failed
                    if ("failed".equals(connectionState)) {
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

            if ((Kind.audio.value.equals(kind) && !canSendMic) || (Kind.video.value.equals(kind) && !canSendCam)) {
                handler.reject(403, "I do not want to consume");
                return;
            }

            Consumer consumer =
                    mRecvTransport.consume(
                            c -> {
                                mStore.removeWrapper(false, c.getId());
                                Logger.e(TAG, "onTransportClose for consume");
                            },
                            id,
                            producerId,
                            kind,
                            rtpParameters,
                            appData);

            mStore.addWrapper(false, peerId, consumer.getId(), kind, consumer);
            if (producerPaused)
                mStore.setWrapperPaused(consumer.getId(), Originator.remote);

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
