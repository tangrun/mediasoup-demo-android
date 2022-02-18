package com.tangrun.mschat;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupClient;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.lv.RoomStore;

import java.util.Arrays;
import java.util.List;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 17:39
 */
public class Start {
    private static String HOST;
    private static String PORT;

    static boolean init = false;

    public static void init(Application application, String host, String port, boolean debug) {
        if (init) return;
        init = true;
        HOST = host;
        PORT = port;
        if (debug) {
            Logger.setLogLevel(Logger.LogLevel.LOG_DEBUG);
            Logger.setDefaultHandler();
        }
        MediasoupClient.initialize(application);
    }

    private static final String TAG = "MS_UIRoomStore";
    private static UIRoomStore uiRoomStore;

    public static UIRoomStore getCurrent() {
        return uiRoomStore;
    }

    public static class User {
        private String id, displayName, avatar;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public JSONObject toJsonObj() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", id);
                jsonObject.put("displayName", displayName);
                jsonObject.put("avatar", avatar);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }
    }

    public static void openCallActivity(Context context) {
        context.startActivity(new Intent(context, RoomActivity.class));
    }

    public static void stopCall(){
        uiRoomStore = null;
    }

    public static void startSingleCall(Context context, String roomId, User me, boolean audioOnly, @Nullable User target) {
        startCall(context, roomId,me, audioOnly, false, target == null ? null : Arrays.asList(target));
    }

    public static void startMultiCall(Context context,String roomId,  User me, boolean audioOnly, @Nullable List<User> inviteUser) {
        startCall(context,roomId, me, audioOnly, true, inviteUser);
    }

    private static void startCall(Context context,String roomId, User me, boolean audioOnly, boolean multi, List<User> inviteUser) {
        if (getCurrent() != null) {
            Log.d(TAG, "startCall: ");
            return;
        }
        RoomStore roomStore = new RoomStore();
        RoomOptions roomOptions = new RoomOptions();
        roomOptions.serverHost = HOST;
        roomOptions.serverPort = PORT;
        roomOptions.mineAvatar = me.getAvatar();
        roomOptions.mineDisplayName = me.getDisplayName();
        roomOptions.mineId = me.getId();
        roomOptions.roomId = roomId;
        roomOptions.mProduceVideo = !audioOnly;
        roomOptions.mConsumeVideo = !audioOnly;
        RoomClient roomClient = new RoomClient(context, roomStore, roomOptions);
        uiRoomStore = new UIRoomStore((Application) context.getApplicationContext(), roomClient);
        uiRoomStore.roomType = multi ? 1 : 0;
        uiRoomStore.firstConnectedAutoJoin = false;
        uiRoomStore.firstJoinedAutoProduce = false;
        uiRoomStore.audioOnly = audioOnly;
        uiRoomStore.addBuddy(inviteUser);
        roomClient.connect();
        openCallActivity(context);
    }

}
