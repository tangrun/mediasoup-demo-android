package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.mediasoup.droid.lib.enums.ConnectionState;
import org.mediasoup.droid.lib.enums.ConversationState;
import org.mediasoup.droid.lib.lv.SupplierMutableLiveData;

import java.util.HashSet;
import java.util.Set;

public class Buddy {


    boolean isProducer;
    private String id;
    private String displayName;
    private DeviceInfo device;
    /**
     * 新增
     */
    private String avatar;
    private Integer volume;
    private final Set<String> ids = new HashSet<>();
    private ConnectionState connectionState = ConnectionState.NEW;
    private ConversationState conversationState = ConversationState.New;

    /**
     * 属性变化
     * ids volume connectionState conversationState
     */
//    private final SupplierMutableLiveData<Buddy> buddyMutableLiveData;

    public Buddy(boolean isProducer, String id, String name, String avatar, DeviceInfo deviceInfo) {
        this.isProducer = isProducer;
        setDisplayName(name);
        setAvatar(avatar);
        setDevice(deviceInfo);
        setId(id);
//        buddyMutableLiveData = new SupplierMutableLiveData<>(() -> Buddy.this);
    }

    public Buddy(boolean isProducer, @NonNull JSONObject info) {
        this.isProducer = isProducer;
        id = info.optString("id");
        displayName = info.optString("displayName");
        JSONObject deviceInfo = info.optJSONObject("device");
        if (deviceInfo != null) {
            device =
                    new DeviceInfo()
                            .setFlag(deviceInfo.optString("flag"))
                            .setName(deviceInfo.optString("name"))
                            .setVersion(deviceInfo.optString("version"));
        } else {
            device = DeviceInfo.unknownDevice();
        }
        avatar = info.optString("avatar");
        connectionState = ConnectionState.get(info.optString("connectionState"));
        conversationState = ConversationState.get(info.optString("conversationState"));
//        buddyMutableLiveData = new SupplierMutableLiveData<>(this);
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public Buddy setConnectionState(ConnectionState connectionState) {
        this.connectionState = connectionState;
        return this;
    }

    public ConversationState getConversationState() {
        return conversationState;
    }

    public Buddy setConversationState(ConversationState conversationState) {
        this.conversationState = conversationState;
        return this;
    }

//    public SupplierMutableLiveData<Buddy> getBuddyLiveData() {
//        return buddyMutableLiveData;
//    }

    public Buddy setProducer(boolean producer) {
        isProducer = producer;
        return this;
    }

    public Integer getVolume() {
        return volume;
    }

    public Buddy setVolume(Integer volume) {
        this.volume = volume;
        return this;
    }

    public boolean isProducer() {
        return isProducer;
    }

    public String getId() {
        return id;
    }

    public Buddy setId(String id) {
        this.id = id;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Buddy setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getAvatar() {
        return avatar;
    }

    public Buddy setAvatar(String avatar) {
        this.avatar = avatar;
        return this;
    }

    public DeviceInfo getDevice() {
        return device;
    }

    public Buddy setDevice(DeviceInfo device) {
        this.device = device;
        return this;
    }

    public Set<String> getIds() {
        return ids;
    }
}
