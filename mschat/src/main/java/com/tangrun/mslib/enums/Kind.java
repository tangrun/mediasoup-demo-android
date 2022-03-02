package com.tangrun.mslib.enums;

/**
 * consumer producer 的 音视频类别
 */
public enum Kind {
    video("video"),
    audio("audio"),
    ;
    public String value;

    Kind(String value) {
        this.value = value;
    }
}

