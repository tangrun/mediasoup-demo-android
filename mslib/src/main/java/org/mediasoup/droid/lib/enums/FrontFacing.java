package org.mediasoup.droid.lib.enums;

/**
 * 摄像头前后
 */
public enum FrontFacing {
    front ("front"),
    rear ("rear"),
    ;
    public final String value;

    FrontFacing(String value) {
        this.value = value;
    }
}
