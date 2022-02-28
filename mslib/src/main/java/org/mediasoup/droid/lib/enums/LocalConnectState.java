package org.mediasoup.droid.lib.enums;

public enum LocalConnectState  {
    // initial state.
    NEW("NEW"),
    // connecting or reconnecting.
    CONNECTING("CONNECTING"),
    RECONNECTING("RECONNECTING"),
    DISCONNECTED("DISCONNECTED"),
    // connected.
    CONNECTED("CONNECTED"),
    // 进入房间
    JOINED("JOINED"),
    // mClosed.
    CLOSED("CLOSED"),
    ;

    public final String value;

    LocalConnectState(String value) {
        this.value = value;
    }

    public static LocalConnectState get(String value){
        for (LocalConnectState state : values()) {
            if (state.value.equals(value))return state;
        }
        return NEW;
    }

    @Override
    public String toString() {
        return value;
    }
}