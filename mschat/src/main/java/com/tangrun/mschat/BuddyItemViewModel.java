package com.tangrun.mschat;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.TrackInvoker;
import org.mediasoup.droid.lib.lv.ChangedMutableLiveData;
import org.mediasoup.droid.lib.model.Buddy;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 10:27
 */
public class BuddyItemViewModel {

    ChangedMutableLiveData<VideoTrack> mVideoTrack = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<AudioTrack> mAudioTrack = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Boolean> mDisabledCam = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Boolean> mDisabledMic = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Integer> mAudioScore = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Integer> mVideoScore = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Integer> mVolume = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<String> mStateTip = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Buddy.ConnectionState> connectionState = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Buddy.ConversationState> conversationState = new ChangedMutableLiveData<>();


    public Buddy buddy;
    private RoomClient roomClient;
    Observer<Buddy> buddyObserver = new Observer<Buddy>() {
        @Override
        public void onChanged(Buddy buddy) {
            TrackInvoker trackInvoker = buddy.isProducer() ? roomClient.getStore().getProducers() : roomClient.getStore().getConsumers();

            AudioTrack audioTrack = trackInvoker.getAudioTrack(buddy.getIds());
            VideoTrack videoTrack = trackInvoker.getVideoTrack(buddy.getIds());
            boolean disabledMic = roomClient.getOptions().mConsumeAudio && audioTrack == null;
            boolean disabledCam = roomClient.getOptions().mConsumeVideo && videoTrack == null;

            mVolume.applySet(buddy.getVolume());
            mAudioTrack.applySet(audioTrack);
            mVideoTrack.applySet(videoTrack);
            mDisabledCam.applySet(disabledCam);
            mDisabledMic.applySet(disabledMic);
            connectionState.applySet(buddy.getConnectionState());
            conversationState.applySet(buddy.getConversationState());
        }
    };

    public void disconnect() {
        if (buddy != null) {
            buddy.getBuddyMutableLiveData().removeObserver(buddyObserver);
        }
    }

    public void connect(LifecycleOwner owner, Buddy buddy, RoomClient roomClient) {
        this.roomClient = roomClient;
        this.buddy = buddy;
        buddy.getBuddyMutableLiveData().removeObserver(buddyObserver);
        buddy.getBuddyMutableLiveData().observe(owner, buddyObserver);
    }

}
