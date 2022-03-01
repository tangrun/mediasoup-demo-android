package com.tangrun.mschat.enums;

public enum CallEnd {
    End("通话已结束"),
    Cancel("通话已取消"),
    Busy("忙线未接听"),
    NoAnswer("无人接听"),
    Reject("通话未接听")

    ;
    public final String desc;

    CallEnd(String desc) {
        this.desc = desc;
    }


}
