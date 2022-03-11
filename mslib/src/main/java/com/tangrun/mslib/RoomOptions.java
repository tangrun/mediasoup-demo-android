package com.tangrun.mslib;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.tangrun.mslib.model.DeviceInfo;
import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

public class RoomOptions implements Parcelable {

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

    public boolean forceH264 = false;
    public boolean forceVP9 = false;

    public String getProtooUrl(JSONArray peers) {
        Map<String, Object> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("peerId", mineId);
        params.put("displayName", mineDisplayName);
        params.put("avatar", mineAvatar);
        params.put("forceH264", forceH264);
        params.put("forceVP9", forceVP9);
        params.put("device", DeviceInfo.androidDevice().toJSONObject().toString());
        if (peers != null && peers.length() > 0)
            params.put("peers", peers.toString());

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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.serverHost);
        dest.writeString(this.serverPort);
        dest.writeString(this.roomId);
        dest.writeString(this.mineId);
        dest.writeString(this.mineDisplayName);
        dest.writeString(this.mineAvatar);
        dest.writeByte(this.defaultFrontCam ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mForceTcp ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mProduce ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mProduceAudio ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mProduceVideo ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mConsume ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mConsumeAudio ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mConsumeVideo ? (byte) 1 : (byte) 0);
        dest.writeByte(this.forceH264 ? (byte) 1 : (byte) 0);
        dest.writeByte(this.forceVP9 ? (byte) 1 : (byte) 0);
    }

    public void readFromParcel(Parcel source) {
        this.serverHost = source.readString();
        this.serverPort = source.readString();
        this.roomId = source.readString();
        this.mineId = source.readString();
        this.mineDisplayName = source.readString();
        this.mineAvatar = source.readString();
        this.defaultFrontCam = source.readByte() != 0;
        this.mForceTcp = source.readByte() != 0;
        this.mProduce = source.readByte() != 0;
        this.mProduceAudio = source.readByte() != 0;
        this.mProduceVideo = source.readByte() != 0;
        this.mConsume = source.readByte() != 0;
        this.mConsumeAudio = source.readByte() != 0;
        this.mConsumeVideo = source.readByte() != 0;
        this.forceH264 = source.readByte() != 0;
        this.forceVP9 = source.readByte() != 0;
    }

    public RoomOptions() {
    }

    protected RoomOptions(Parcel in) {
        this.serverHost = in.readString();
        this.serverPort = in.readString();
        this.roomId = in.readString();
        this.mineId = in.readString();
        this.mineDisplayName = in.readString();
        this.mineAvatar = in.readString();
        this.defaultFrontCam = in.readByte() != 0;
        this.mForceTcp = in.readByte() != 0;
        this.mProduce = in.readByte() != 0;
        this.mProduceAudio = in.readByte() != 0;
        this.mProduceVideo = in.readByte() != 0;
        this.mConsume = in.readByte() != 0;
        this.mConsumeAudio = in.readByte() != 0;
        this.mConsumeVideo = in.readByte() != 0;
        this.forceH264 = in.readByte() != 0;
        this.forceVP9 = in.readByte() != 0;
    }

    public static final Creator<RoomOptions> CREATOR = new Creator<RoomOptions>() {
        @Override
        public RoomOptions createFromParcel(Parcel source) {
            return new RoomOptions(source);
        }

        @Override
        public RoomOptions[] newArray(int size) {
            return new RoomOptions[size];
        }
    };
}
