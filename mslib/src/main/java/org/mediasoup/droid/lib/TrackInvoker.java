package org.mediasoup.droid.lib;

import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.Collection;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 23:00
 */
public interface TrackInvoker {
    AudioTrack getAudioTrack(Collection<String> ids);
    VideoTrack getVideoTrack(Collection<String> ids);
}
