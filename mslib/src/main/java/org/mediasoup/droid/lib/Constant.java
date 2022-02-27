package org.mediasoup.droid.lib;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 11:30
 */
public class Constant {

    /**
     * consumer producer 的 音视频类别
     */
    public interface Kind {
        String video = "video";
        String audio = "audio";
    }

    /**
     * 摄像头前后
     */
    public interface FrontFacing {
        String front = "front";
        String rear = "rear";
    }

    /**
     * consumer的 paused resumed 操作者  对方remote 本地local
     */
    public enum Originator {
        local("local"),
        remote("remote"),
        ;
        String value;

        Originator(String value) {
            this.value = value;
        }
    }

    public enum ConnectionState {
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

        ConnectionState(String value) {
            this.value = value;
        }

        String value;

        @Override
        public String toString() {
            return value;
        }
    }

    public enum CameraState {
        disabled,
        enabled,
        inProgress,

    }

    public enum CameraFacingState {
        front,
        rear,
        inProgress,
    }

    public enum MicrophoneState {
        disabled,
        enabled,
        inProgress,

    }
}
