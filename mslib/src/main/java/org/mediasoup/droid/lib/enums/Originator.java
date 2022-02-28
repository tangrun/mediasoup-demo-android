package org.mediasoup.droid.lib.enums;

/**
 * consumer的 paused resumed 操作者  对方remote 本地local
 */
public enum Originator {
    local("local"),
    remote("remote"),
    ;
    public final String value;

    Originator(String value) {
        this.value = value;
    }
}

