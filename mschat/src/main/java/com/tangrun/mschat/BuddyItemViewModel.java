package com.tangrun.mschat;

import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import org.mediasoup.droid.lib.Constant;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.CommonInvoker;
import org.mediasoup.droid.lib.WrapperCommon;
import org.mediasoup.droid.lib.lv.ChangedMutableLiveData;
import org.mediasoup.droid.lib.model.Buddy;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;

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
    ChangedMutableLiveData<Integer> mAudioPScore = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Integer> mAudioCScore = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Integer> mVideoPScore = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Integer> mVideoCScore = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Integer> mVolume = new ChangedMutableLiveData<>(null);
    ChangedMutableLiveData<String> mStateTip = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Buddy.ConnectionState> connectionState = new ChangedMutableLiveData<>();
    ChangedMutableLiveData<Buddy.ConversationState> conversationState = new ChangedMutableLiveData<>();


    public Buddy buddy;
    private RoomClient roomClient;
    private DisposableObserver<Long> disposableObserver;


    Observer<Buddy> buddyObserver = new Observer<Buddy>() {
        @Override
        public void onChanged(Buddy buddy) {
            CommonInvoker commonInvoker = buddy.isProducer() ? roomClient.getStore().getProducers() : roomClient.getStore().getConsumers();

            WrapperCommon videoCommon = commonInvoker.getCommonInfo(buddy.getIds(), Constant.kind_video);
            WrapperCommon audioCommon = commonInvoker.getCommonInfo(buddy.getIds(), Constant.kind_audio);



            AudioTrack audioTrack = audioCommon == null ? null : audioCommon.getTrack();
            VideoTrack videoTrack = videoCommon == null ? null : videoCommon.getTrack();
            boolean disabledMic = roomClient.getOptions().mConsumeAudio && audioTrack == null;
            boolean disabledCam = roomClient.getOptions().mConsumeVideo && videoTrack == null;

            mVolume.applySet(buddy.getVolume());
            if (buddy.getVolume() != null) {
                if (disposableObserver != null && !disposableObserver.isDisposed()) {
                    disposableObserver.dispose();
                    disposableObserver = null;
                }

                Observable.timer(1500, TimeUnit.MILLISECONDS)
                        .subscribe(new DisposableObserver<Long>() {
                                       @Override
                                       public void onNext(@NonNull Long aLong) {

                                       }

                                       @Override
                                       public void onError(@NonNull Throwable e) {

                                       }

                                       @Override
                                       public void onComplete() {
                                           mVolume.applySet(null);
                                           if (!isDisposed()) {
                                               dispose();
                                           }
                                       }
                                   }
                        );

            }

            mAudioTrack.applySet(audioTrack);
            mVideoTrack.applySet(videoTrack);
            mDisabledCam.applySet(disabledCam);
            mDisabledMic.applySet(disabledMic);
            connectionState.applySet(buddy.getConnectionState());
            conversationState.applySet(buddy.getConversationState());
            mAudioPScore.applySet(audioCommon == null ? null : audioCommon.getProducerScore());
            mAudioCScore.applySet(audioCommon == null ? null : audioCommon.getConsumerScore());
            mVideoPScore.applySet(videoCommon == null ? null : videoCommon.getProducerScore());
            mVideoCScore.applySet(videoCommon == null ? null : videoCommon.getConsumerScore());
        }
    };

    public void disconnect() {
        if (buddy != null) {
            buddy.getBuddyLiveData().removeObserver(buddyObserver);
        }
    }

    public void connect(LifecycleOwner owner, Buddy buddy, RoomClient roomClient) {
        this.roomClient = roomClient;
        this.buddy = buddy;
        buddy.getBuddyLiveData().removeObserver(buddyObserver);
        buddy.getBuddyLiveData().observe(owner, buddyObserver);
    }

}
