package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.mediasoup.droid.lib.lv.SupplierMutableLiveData;

import java.util.HashSet;
import java.util.Set;

public class Buddy {

    public enum ConnectionState {
        /**
         * 初始化
         */
        NEW("New"),

        /**
         * socket连接上
         */
        Online("Online"),

        /**
         * socket连接上
         */
        Offline("Offline"),

        /**
         * 主动离开
         */
        Left("Left"),
        ;
        String value;

        ConnectionState(String value) {
            this.value = value;
        }
        public static ConnectionState get(String value){
            for (ConnectionState state : values()) {
                if (state.value.equals(value))return state;
            }
            return NEW;
        }


        @Override
        public String toString() {
            return value;
        }
    }


    public enum ConversationState {
        /**
         * 初始化
         */
        New("New"),

        /**
         * 被邀请中
         */
        Invited("Invited"),

        /**
         * 被邀请时 超时未接听
         */
        InviteTimeout("InviteTimeout"),

        /**
         * 被邀请时 拒绝接听
         */
        InviteReject("InviteReject"),

        /**
         * 被邀请时 忙线中
         */
        InviteBusy("InviteBusy"),

        /**
         * 开始通话
         */
        Joined("Joined"),

        /**
         * 挂断离开
         */
        Left("Left"),
        ;
        String value;

        ConversationState(String value) {
            this.value = value;
        }

        public static ConversationState get(String value){
            for (ConversationState state : values()) {
                if (state.value.equals(value))return state;
            }
            return New;
        }

        @Override
        public String toString() {
            return value;
        }
    }

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
    private final SupplierMutableLiveData<Buddy> buddyMutableLiveData;

    public Buddy(boolean isProducer, String id, String name, String avatar, DeviceInfo deviceInfo) {
        this.isProducer = isProducer;
        setDisplayName(name);
        setAvatar(avatar);
        setDevice(deviceInfo);
        setId(id);
        buddyMutableLiveData = new SupplierMutableLiveData<>(() -> Buddy.this);
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
        buddyMutableLiveData = new SupplierMutableLiveData<>(this);
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

    public SupplierMutableLiveData<Buddy> getBuddyLiveData() {
        return buddyMutableLiveData;
    }

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
