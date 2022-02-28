package org.mediasoup.droid.lib.enums;

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
    public final String value;

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