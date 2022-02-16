package org.mediasoup.droid.lib;

import androidx.annotation.NonNull;

import org.mediasoup.droid.lib.model.Buddy;
import org.mediasoup.droid.lib.model.DeviceInfo;

import java.util.Locale;

public class RoomOptions {

    public String serverHost;
    public String serverPort;
    public String roomId;
    public Buddy me;
    public boolean defaultFrontCam = true;
    public boolean connectedJoin;
    // Whether we want to force RTC over TCP.
    public boolean mForceTcp = false;
    // Whether we want to produce audio/video.
    public boolean mProduce = true;
    public boolean mProduceAudio = true;
    public boolean mProduceVideo = true;
    // Whether we should consume.
    public boolean mConsume = true;
    public boolean mConsumeAudio = true;
    public boolean mConsumeVideo = true;
    // Whether we want DataChannels.
    public boolean mUseDataChannel = false;

    public boolean forceH264 = false;
    public boolean forceVP9 = false;

    public void setMe(String id, String name, String avatar) {
        me = new Buddy(true, id, name, avatar, DeviceInfo.androidDevice());
    }

    public String getProtooUrl() {
        String url =
                String.format(
                        Locale.US, "wss://%s:%s/?roomId=%s&peerId=%s&displayName=%s&avatar=%s", serverHost, serverPort,
                        roomId, me.getId(), me.getDisplayName(), me.getAvatar());
        if (forceH264) {
            url += "&forceH264=true";
        } else if (forceVP9) {
            url += "&forceVP9=true";
        }
        return url;
    }
}
