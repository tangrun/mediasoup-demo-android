package com.tangrun.mschat;

import android.app.Application;
import android.content.Context;
import android.os.Parcel;
import com.tangrun.mschat.enums.CallEnd;
import com.tangrun.mschat.enums.RoomType;
import com.tangrun.mschat.model.UIRoomStore;
import com.tangrun.mschat.model.User;
import com.tangrun.mslib.RoomOptions;
import com.tangrun.mslib.utils.ArchTaskExecutor;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupClient;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 17:39
 */
public class MSManager {
    private static final String TAG = "MS_UIRoomStore";

    private static MSApi msApi;
    private static UICallback uiCallback;
    private static boolean init = false;

    public static void init(Application application, String host, String port, boolean debug, UICallback uiCallback) {
        if (init) return;
        init = true;
        setServer(host, port);
        MSManager.uiCallback = uiCallback;
        if (debug) {
            Logger.setLogLevel(Logger.LogLevel.LOG_DEBUG);
            Logger.setDefaultHandler();
        }
        MediasoupClient.initialize(application);
    }

    public static void setServer(String host, String port) {
        msApi = new MSApi(host, port);
    }

    public static UIRoomStore getCurrent() {
        return UIRoomStore.getCurrent();
    }

    public static void addUser(List<User> list) {
        if (getCurrent() != null && list != null && !list.isEmpty())
            getCurrent().addUser(list);
    }

    public static void startCall(Context context, String roomId, User me,
                                 boolean audioOnly, boolean multi, boolean owner,
                                 List<User> inviteUser) {
        startCall(context, msApi.getHost(), msApi.getPort(), roomId, me, audioOnly, multi, owner, inviteUser, uiCallback);
    }

    public static void startCall(Context context, String host, String port, String roomId, User me,
                                 boolean audioOnly, boolean multi, boolean owner,
                                 List<User> inviteUser, UICallback uiCallback) {
        if (getCurrent() != null) {
            Logger.d(TAG, "startCall: ");
            return;
        }
        RoomOptions roomOptions = new RoomOptions();
        // 服务器地址
        roomOptions.serverHost = host;
        roomOptions.serverPort = port;
        // 房间id
        roomOptions.roomId = roomId;
        // 我的信息
        roomOptions.mineAvatar = me.getAvatar();
        roomOptions.mineDisplayName = me.getDisplayName();
        roomOptions.mineId = me.getId();
        // 流的开关
        roomOptions.mConsume = true;
        roomOptions.mConsumeAudio = true;
        roomOptions.mConsumeVideo = !audioOnly;
        roomOptions.mProduce = true;
        roomOptions.mProduceAudio = true;
        roomOptions.mProduceVideo = !audioOnly;
        // 房间信息and状态
        UIRoomStore uiRoomStore = new UIRoomStore();
        // 房间信息
        uiRoomStore.roomType = multi ? RoomType.MultiCall : RoomType.SingleCall;
        uiRoomStore.owner = owner;
        uiRoomStore.audioOnly = audioOnly;
        // 回调
        uiRoomStore.uiCallback = uiCallback;
        // 配置
        uiRoomStore.firstSpeakerOn = !audioOnly || multi;
        uiRoomStore.firstConnectedAutoJoin = owner;
        uiRoomStore.firstJoinedAutoProduceAudio = true;
        uiRoomStore.firstJoinedAutoProduceVideo = !audioOnly && !multi;
        // 初始化 开始通话
        uiRoomStore.init(context, roomOptions);
        uiRoomStore.connect(inviteUser);
        uiRoomStore.openCallActivity();
        saveCurrentCallToLocal(context, uiRoomStore);
    }

    //region 通话持久化
    public static void resumeLastInterruptCall(Context context) {
        ArchTaskExecutor.getIOThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                FileInputStream fis;
                try {
                    fis = context.openFileInput("MS_CALL");
                    byte[] bytes = new byte[fis.available()];
                    fis.read(bytes);
                    fis.close();
                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(bytes, 0, bytes.length);
                    parcel.setDataPosition(0);
                    UIRoomStore uiRoomStore = parcel.readParcelable(UIRoomStore.class.getClassLoader());
                    if (uiRoomStore != null) {
                        if (getCurrent() != null) {
                            if (uiCallback != null)
                                uiCallback.onCallEnd(uiRoomStore.getRoomOptions().roomId, uiRoomStore.roomType, uiRoomStore.audioOnly, CallEnd.End, null, null);
                            return;
                        }
                        new MSApi(uiRoomStore.getRoomOptions().serverHost, uiRoomStore.getRoomOptions().serverPort)
                                .roomExists(uiRoomStore.getRoomOptions().roomId, new ApiCallback<Boolean>() {
                                    @Override
                                    public void onFail(Throwable e) {
                                        uiCallback.onCallEnd(uiRoomStore.getRoomOptions().roomId, uiRoomStore.roomType, uiRoomStore.audioOnly, CallEnd.End, null, null);
                                        saveCurrentCallToLocal(context,null);
                                    }

                                    @Override
                                    public void onSuccess(Boolean aBoolean) {
                                        if (aBoolean == Boolean.TRUE) {
                                            uiRoomStore.init(context, uiRoomStore.getRoomOptions());
                                            uiRoomStore.connect(null);
                                            uiRoomStore.openCallActivity();
                                        } else onFail(null);
                                    }
                                });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void saveCurrentCallToLocal(Context context, UIRoomStore uiRoomStore) {
        ArchTaskExecutor.getIOThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fos;
                try {
                    fos = context.openFileOutput("MS_CALL", Context.MODE_PRIVATE);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    Parcel parcel = Parcel.obtain();
                    parcel.writeParcelable(uiRoomStore, 0);
                    bos.write(parcel.marshall());
                    bos.flush();
                    bos.close();
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //endregion


    //region 接口调用
    public static void roomExists(String id, ApiCallback<Boolean> apiCallback) {
        msApi.roomExists(id, apiCallback);
    }

    public static void busy(String roomId, String userId, ApiCallback<Object> apiCallback) {
        msApi.busy(roomId, userId, apiCallback);
    }

    public static void createRoom(String id, ApiCallback<String> apiCallback) {
        msApi.createRoom(id, apiCallback);
    }

    public static void createRoom(ApiCallback<String> apiCallback) {
        msApi.createRoom(apiCallback);
    }
    //endregion

    //region 拓展方法
    public static void setCallEnd(String id, RoomType roomType, boolean audioOnly, CallEnd callEnd) {
        if (uiCallback != null) {
            uiCallback.onCallEnd(id, roomType, audioOnly, callEnd, null, null);
        }
    }

    public static void busyForUICallback(String roomId, String userId, RoomType roomType, boolean audioOnly) {
        msApi.busy(roomId, userId, new ApiCallback<Object>() {
            @Override
            public void onFail(Throwable e) {
                setCallEnd(roomId, roomType, audioOnly, CallEnd.Busy);
            }

            @Override
            public void onSuccess(Object o) {
                setCallEnd(roomId, roomType, audioOnly, CallEnd.Busy);
            }
        });
    }
    //endregion
}
