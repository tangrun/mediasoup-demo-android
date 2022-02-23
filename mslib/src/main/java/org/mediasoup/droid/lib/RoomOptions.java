package org.mediasoup.droid.lib;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import org.mediasoup.droid.lib.model.Buddy;
import org.mediasoup.droid.lib.model.DeviceInfo;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RoomOptions {

    public String serverHost;
    public String serverPort;
    public String roomId;
    public String mineId;
    public String mineDisplayName;
    public String mineAvatar;
    public boolean defaultFrontCam = true;
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

    public String getProtooUrl() {
        Map<String, Object> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("peerId", mineId);
        params.put("displayName", mineDisplayName);
        params.put("avatar", mineAvatar);
        params.put("forceH264", forceH264);
        params.put("forceVP9", forceVP9);
        params.put("device", DeviceInfo.androidDevice().toJSONObject().toString());

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wss://")
                .append(serverHost);
        if (serverPort != null && serverPort.trim().length() > 0) {
            stringBuilder.append(":")
                    .append(serverPort);
        }
        String a = "?";
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            stringBuilder.append(a)
                    .append(entry.getKey())
                    .append("=")
                    .append(Uri.encode(String.valueOf(entry.getValue())));
            a = "&";
        }
        return stringBuilder.toString();
    }
}
