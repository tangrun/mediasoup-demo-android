package com.tangrun.mslib.model;

import android.os.Build;
import org.json.JSONObject;
import org.mediasoup.droid.BuildConfig;

import static com.tangrun.mslib.utils.JsonUtils.jsonPut;

@SuppressWarnings("WeakerAccess")
public class DeviceInfo {

    private String mFlag;
    private String mName;
    private String mVersion;

    public String getFlag() {
        return mFlag;
    }

    public DeviceInfo setFlag(String flag) {
        this.mFlag = flag;
        return this;
    }

    public String getName() {
        return mName;
    }

    public DeviceInfo setName(String name) {
        this.mName = name;
        return this;
    }

    public String getVersion() {
        return mVersion;
    }

    public DeviceInfo setVersion(String version) {
        this.mVersion = version;
        return this;
    }

    public static DeviceInfo androidDevice() {
        return new DeviceInfo()
                .setFlag("android")
                .setName(
                        Build.BRAND + "|" +
                                Build.MODEL + "|" +
                                Build.VERSION.RELEASE + "|" +
                                Build.VERSION.SDK_INT
                )
                .setVersion(BuildConfig.VERSION_NAME + "|" + BuildConfig.VERSION_CODE);
    }

    public static DeviceInfo unknownDevice() {
        return new DeviceInfo().setFlag("unknown").setName("unknown").setVersion("unknown");
    }

    public JSONObject toJSONObject() {
        JSONObject deviceInfo = new JSONObject();
        jsonPut(deviceInfo, "flag", getFlag());
        jsonPut(deviceInfo, "name", getName());
        jsonPut(deviceInfo, "version", getVersion());
        return deviceInfo;
    }
}
