package com.tangrun.mschat;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.lv.RoomStore;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 17:39
 */
public class Start {
    private static final String TAG = "Start";
    static final String host = "192.168.0.218";
    static final String port = "4443";

    static RoomClient client;


    public static RoomClient getClient(){
        return client;
    }

    public static void stopCall() {
        client = null;
    }

    public static void startMultiCall(Context context, String roomId, String id, String name, String avatar) {
        if (client != null){
            Log.d(TAG, "startMultiCall: 已有通话");
            return;
        }
        Logger.setDefaultHandler();
        Logger.setLogLevel(Logger.LogLevel.LOG_DEBUG);

        RoomOptions options = new RoomOptions();
        options.setMe(id, name, avatar);
        options.serverHost = host;
        options.serverPort = port;
        options.roomId = roomId;

        RoomStore roomStore = new RoomStore();

        client = new RoomClient(context, roomStore, options);
        client.connect(true);
        context.startActivity(new Intent(context,RoomActivity.class));
    }

}
