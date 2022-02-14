package com.tangrun.mschat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;
import androidx.lifecycle.LifecycleOwner;
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

import java.util.Collection;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 10:27
 */
public class BuddyItemViewModel extends RoomStoreViewModel {

    ObservableField<Buddy> mBuddy = new ObservableField<>();
    ObservableField<VideoTrack> mVideoTrack = new ObservableField<>();
    ObservableField<AudioTrack> mAudioTrack = new ObservableField<>();
    ObservableField<Boolean> mDisabledCam = new ObservableField<>();
    ObservableField<Boolean> mDisabledMic = new ObservableField<>();
    ObservableField<Integer> mAudioScore = new ObservableField<>();
    ObservableField<Integer> mVideoScore = new ObservableField<>();
    ObservableField<Integer> mVolume = new ObservableField<>();
    ObservableField<String> mStateTip = new ObservableField<>();


    public BuddyItemViewModel(@NonNull Application application) {
        super(application);
    }

    public void onItemClick() {

    }

    Observer<Buddy> buddyObserver = new Observer<Buddy>() {
        @Override
        public void onChanged(Buddy buddy) {
            mVolume.set(buddy.getVolume());

            TrackInvoker trackInvoker = buddy.isProducer() ? getRoomStore().getProducers() : getRoomStore().getConsumers();
            AudioTrack audioTrack = trackInvoker.getAudioTrack(buddy.getIds());
            VideoTrack videoTrack = trackInvoker.getVideoTrack(buddy.getIds());

            boolean disabledMic = getRoomOptions().mConsumeAudio && audioTrack == null;
            boolean disabledCam = getRoomOptions().mConsumeVideo && videoTrack == null;

            mAudioTrack.set(audioTrack);
            mVideoTrack.set(videoTrack);
            mDisabledCam.set(disabledCam);
            mDisabledMic.set(disabledMic);
        }
    };

    public void connect(LifecycleOwner owner, String peerId) {
        Buddy buddy = getRoomStore().getBuddys().getValue().getBuddy(peerId);
        if (buddy == null) return;

        buddy.getBuddyMutableLiveData().removeObserver(buddyObserver);
        buddy.getBuddyMutableLiveData().observe(owner, buddyObserver);
    }

    public ObservableField<Buddy> getBuddy() {
        return mBuddy;
    }

    public ObservableField<VideoTrack> getVideoTrack() {
        return mVideoTrack;
    }

    public ObservableField<AudioTrack> getAudioTrack() {
        return mAudioTrack;
    }

    public ObservableField<Boolean> getDisabledCam() {
        return mDisabledCam;
    }

    public ObservableField<Boolean> getDisabledMic() {
        return mDisabledMic;
    }

    public ObservableField<Integer> getAudioScore() {
        return mAudioScore;
    }

    public ObservableField<Integer> getVideoScore() {
        return mVideoScore;
    }

    public ObservableField<Integer> getVolume() {
        return mVolume;
    }

    public ObservableField<String> getStateTip() {
        return mStateTip;
    }
}
