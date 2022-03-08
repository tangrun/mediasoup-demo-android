package com.tangrun.mschat.enums;

public enum CallEnd {
    None("通话已开始"),
    NetError("网络中断"),
    End("通话已结束"),
    Cancel("通话已取消"),
    RemoteCancel("对方已取消"),
    Busy("忙线未接听"),
    RemoteBusy("对方忙线中"),
    NoAnswer("未接听"),
    RemoteNoAnswer("无人接听"),
    Reject("通话已拒绝"),
    RemoteReject("通话未接听"),
    ;
    public final String desc;

    CallEnd(String desc) {
        this.desc = desc;
    }


}
