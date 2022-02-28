package com.tangrun.mschat.model;

import org.mediasoup.droid.lib.WrapperCommon;
import org.mediasoup.droid.lib.enums.ConnectionState;
import org.mediasoup.droid.lib.enums.ConversationState;
import org.mediasoup.droid.lib.lv.ChangedMutableLiveData;
import org.mediasoup.droid.lib.lv.MultiMutableLiveData;
import org.mediasoup.droid.lib.model.Buddy;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;


/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 10:27
 */
public class BuddyModel {

    public ChangedMutableLiveData<WrapperCommon<?>> videoWrapper = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<VideoTrack> videoTrack = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Boolean> videoPaused = new ChangedMutableLiveData<>();

    public ChangedMutableLiveData<WrapperCommon<?>> audioWrapper = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<AudioTrack> audioTrack = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Boolean> audioPaused = new ChangedMutableLiveData<>();

    public ChangedMutableLiveData<Boolean> disabledCam = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Boolean> disabledMic = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Integer> audioPScore = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Integer> audioCScore = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Integer> videoPScore = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Integer> videoCScore = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<Integer> volume = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<ConnectionState> connectionState = new ChangedMutableLiveData<>();
    public ChangedMutableLiveData<ConversationState> conversationState = new ChangedMutableLiveData<>();
    public MultiMutableLiveData state = new MultiMutableLiveData();

    public Buddy buddy;
//    private RoomClient roomClient;
//    private DisposableObserver<Long> disposableObserver;

    public BuddyModel(Buddy buddy) {
        this.buddy = buddy;
        state.addSource(connectionState);
        state.addSource(conversationState);
    }

//    @Override
//    public void onChanged(Buddy buddy) {
//        this.buddy = buddy;
//
//        WrapperCommon<?> videoCommon = null;
//        WrapperCommon<?> audioCommon=null;
//
//        for (String id : buddy.getIds()) {
//            WrapperCommon<?> wrapperCommon = roomClient.getStore().getWrappers().get(id);
//            if (wrapperCommon ==null)continue;
//            if (Kind.audio.value.equals(wrapperCommon.getKind())){
//                audioCommon = wrapperCommon;
//            }else {
//                videoCommon = wrapperCommon;
//            }
//        }
//
//        AudioTrack audioTrack = audioCommon == null ? null : audioCommon.getTrack();
//        VideoTrack videoTrack = videoCommon == null ? null : videoCommon.getTrack();
//        boolean disabledMic = roomClient.getOptions().mConsumeAudio && audioTrack == null;
//        boolean disabledCam = roomClient.getOptions().mConsumeVideo && videoTrack == null;
//
//        mVolume.applySet(buddy.getVolume());
//        if (buddy.getVolume() != null) {
//            if (disposableObserver != null && !disposableObserver.isDisposed()) {
//                disposableObserver.dispose();
//                disposableObserver = null;
//            }
//
//            Observable.timer(1500, TimeUnit.MILLISECONDS)
//                    .subscribe(new DisposableObserver<Long>() {
//                                   @Override
//                                   public void onNext(@NonNull Long aLong) {
//
//                                   }
//
//                                   @Override
//                                   public void onError(@NonNull Throwable e) {
//
//                                   }
//
//                                   @Override
//                                   public void onComplete() {
//                                       mVolume.applySet(null);
//                                       if (!isDisposed()) {
//                                           dispose();
//                                       }
//                                   }
//                               }
//                    );
//
//        }
//
//        mAudioTrack.applySet(audioTrack);
//        mVideoTrack.applySet(videoTrack);
//        mDisabledCam.applySet(disabledCam);
//        mDisabledMic.applySet(disabledMic);
//        connectionState.applySet(buddy.getConnectionState());
//        conversationState.applySet(buddy.getConversationState());
//        mAudioPScore.applySet(audioCommon == null ? null : audioCommon.getProducerScore());
//        mAudioCScore.applySet(audioCommon == null ? null : audioCommon.getConsumerScore());
//        mVideoPScore.applySet(videoCommon == null ? null : videoCommon.getProducerScore());
//        mVideoCScore.applySet(videoCommon == null ? null : videoCommon.getConsumerScore());
//    }
}
