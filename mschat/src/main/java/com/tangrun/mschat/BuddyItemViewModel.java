package com.tangrun.mschat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.Constant;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.TrackInvoker;
import org.mediasoup.droid.lib.model.Buddy;
import org.mediasoup.droid.lib.model.Buddys;
import org.mediasoup.droid.lib.model.Consumers;
import org.mediasoup.droid.lib.model.Producers;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.security.acl.Owner;
import java.util.Collection;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 10:27
 */
public class BuddyItemViewModel {

    MutableLiveData<VideoTrack> mVideoTrack = new MutableLiveData<>();
    MutableLiveData<AudioTrack> mAudioTrack = new MutableLiveData<>();
    MutableLiveData<Boolean> mDisabledCam = new MutableLiveData<>();
    MutableLiveData<Boolean> mDisabledMic = new MutableLiveData<>();
    MutableLiveData<Integer> mAudioScore = new MutableLiveData<>();
    MutableLiveData<Integer> mVideoScore = new MutableLiveData<>();
    MutableLiveData<Integer> mVolume = new MutableLiveData<>();
    MutableLiveData<String> mStateTip = new MutableLiveData<>();
    MutableLiveData<Buddy.ConnectionState> connectionState = new MutableLiveData<>();
    MutableLiveData<Buddy.ConversationState> conversationState = new MutableLiveData<>();


    public Buddy buddy;
    private RoomViewModel roomViewModel;
    Observer<Buddy> buddyObserver = new Observer<Buddy>() {
        @Override
        public void onChanged(Buddy buddy) {
            mVolume.setValue(buddy.getVolume());

            TrackInvoker trackInvoker = buddy.isProducer() ? roomViewModel.getRoomStore().getProducers() : roomViewModel.getRoomStore().getConsumers();
            AudioTrack audioTrack = trackInvoker.getAudioTrack(buddy.getIds());
            VideoTrack videoTrack = trackInvoker.getVideoTrack(buddy.getIds());

            boolean disabledMic = roomViewModel.getRoomOptions().mConsumeAudio && audioTrack == null;
            boolean disabledCam = roomViewModel.getRoomOptions().mConsumeVideo && videoTrack == null;

            mAudioTrack.setValue(audioTrack);
            mVideoTrack.setValue(videoTrack);
            mDisabledCam.setValue(disabledCam);
            mDisabledMic.setValue(disabledMic);
            connectionState.setValue(buddy.getConnectionState());
            conversationState.setValue(buddy.getConversationState());
        }
    };

    public void disconnect() {
        if (buddy != null) {
            buddy.getBuddyMutableLiveData().removeObserver(buddyObserver);
        }
    }

    public void connect(LifecycleOwner owner, Buddy buddy, RoomViewModel roomViewModel) {
        this.roomViewModel = roomViewModel;
        this.buddy = buddy;
        buddy.getBuddyMutableLiveData().removeObserver(buddyObserver);
        buddy.getBuddyMutableLiveData().observe(owner, buddyObserver);
    }

}
