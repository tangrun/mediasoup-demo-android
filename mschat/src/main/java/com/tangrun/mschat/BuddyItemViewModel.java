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

    public void connect(LifecycleOwner owner, String peerId) {
        Buddy buddy = getRoomStore().getBuddys().getValue().getBuddy(peerId);
        if (buddy == null) return;
        if (buddy.isProducer()) {
            getRoomStore().getProducers().observe(owner, new Observer<Producers>() {
                @Override
                public void onChanged(Producers producers) {
                    Buddy buddy = getRoomStore().getBuddys().getValue().getBuddy(peerId);

                    Producers.ProducersWrapper audioWrapper = getProducer(producers, buddy.getIds(), Constant.kind_audio);
                    Producer audioProducer = audioWrapper == null ? null : audioWrapper.getProducer();
                    Producers.ProducersWrapper videoWrapper = getProducer(producers, buddy.getIds(), Constant.kind_video);
                    Producer videoProducer = videoWrapper == null ? null : videoWrapper.getProducer();

                    AudioTrack audioTrack = audioProducer == null ? null : (AudioTrack) audioProducer.getTrack();
                    boolean disabledMic = getRoomOptions().mProduceAudio && audioTrack == null;
                    VideoTrack videoTrack = videoProducer == null ? null : (VideoTrack) videoProducer.getTrack();
                    boolean disabledCam = getRoomOptions().mProduceVideo && videoTrack == null;

                    mAudioTrack.set(audioTrack);
                    mVideoTrack.set(videoTrack);
                    mDisabledCam.set(disabledCam);
                    mDisabledMic.set(disabledMic);
                }
            });
        } else {
            getRoomStore().getConsumers().observe(owner, new Observer<Consumers>() {
                @Override
                public void onChanged(Consumers consumers) {
                    Buddy buddy = getRoomStore().getBuddys().getValue().getBuddy(peerId);

                    Consumers.ConsumerWrapper audioWrapper = getConsumer(consumers, buddy.getIds(), Constant.kind_audio);
                    Consumer audioProducer = audioWrapper == null ? null : audioWrapper.getConsumer();
                    AudioTrack audioTrack = audioProducer == null ? null : (AudioTrack) audioProducer.getTrack();
                    boolean disabledMic = getRoomOptions().mConsumeAudio && audioTrack == null;

                    Consumers.ConsumerWrapper videoWrapper = getConsumer(consumers, buddy.getIds(), Constant.kind_video);
                    Consumer videoProducer = videoWrapper == null ? null : videoWrapper.getConsumer();
                    VideoTrack videoTrack = videoProducer == null ? null : (VideoTrack) videoProducer.getTrack();
                    boolean disabledCam = getRoomOptions().mConsumeVideo && videoTrack == null;

                    mAudioTrack.set(audioTrack);
                    mVideoTrack.set(videoTrack);
                    mDisabledCam.set(disabledCam);
                    mDisabledMic.set(disabledMic);
                }
            });
        }

        getRoomStore().getBuddys().observe(owner, new Observer<Buddys>() {
            @Override
            public void onChanged(Buddys buddys) {
                Buddy buddy = buddys.getBuddy(peerId);
                mBuddy.set(buddy);
                VideoTrack videoTrack;
                AudioTrack audioTrack;
                boolean disabledCam;
                boolean disabledMic;
                Integer audioScore = null, videoScore = null;

                RoomOptions options = getRoomOptions();
                if (buddy.isProducer()) {
                    Producers.ProducersWrapper audioWrapper = getRoomStore().getProducers().getValue().filter(Constant.kind_audio);
                    Producer audioProducer = audioWrapper == null ? null : audioWrapper.getProducer();
                    audioTrack = audioProducer == null ? null : (AudioTrack) audioProducer.getTrack();
                    disabledMic = options.mProduceAudio && audioTrack == null;

                    Producers.ProducersWrapper videoWrapper = getRoomStore().getProducers().getValue().filter(Constant.kind_video);
                    Producer videoProducer = videoWrapper == null ? null : videoWrapper.getProducer();
                    videoTrack = videoProducer == null ? null : (VideoTrack) videoProducer.getTrack();
                    disabledCam = options.mProduceVideo && videoTrack == null;
                } else {
                    Consumers.ConsumerWrapper audioWrapper = getConsumer(buddy.getIds(), Constant.kind_audio);
                    Consumer audioProducer = audioWrapper == null ? null : audioWrapper.getConsumer();
                    audioTrack = audioProducer == null ? null : (AudioTrack) audioProducer.getTrack();
                    disabledMic = options.mConsumeAudio && audioTrack == null;

                    Consumers.ConsumerWrapper videoWrapper = getConsumer(buddy.getIds(), Constant.kind_video);
                    Consumer videoProducer = videoWrapper == null ? null : videoWrapper.getConsumer();
                    videoTrack = videoProducer == null ? null : (VideoTrack) videoProducer.getTrack();
                    disabledCam = options.mConsumeVideo && videoTrack == null;
                }

                mAudioTrack.set(audioTrack);
                mVideoTrack.set(videoTrack);
                mDisabledCam.set(disabledCam);
                mDisabledMic.set(disabledMic);
                mVolume.set(disabledMic ? null : buddy.getVolume());
                mAudioScore.set(audioScore);
                mVideoScore.set(videoScore);
            }
        });
    }

    Producers.ProducersWrapper getProducer(Producers producers, Collection<String> id, String kind) {
        for (String s : id) {
            Producers.ProducersWrapper wrapper = producers.getProducer(s);
            if (wrapper != null && kind.equals(wrapper.getType())) return wrapper;
        }
        return null;
    }

    Consumers.ConsumerWrapper getConsumer(Consumers consumers, Collection<String> id, String kind) {
        for (String s : id) {
            Consumers.ConsumerWrapper wrapper = consumers.getConsumer(s);
            if (wrapper != null && kind.equals(wrapper.getType())) return wrapper;
        }
        return null;
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
