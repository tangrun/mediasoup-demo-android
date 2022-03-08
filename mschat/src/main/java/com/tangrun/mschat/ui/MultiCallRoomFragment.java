package com.tangrun.mschat.ui;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.R;
import com.tangrun.mschat.databinding.MsFragmentMultiCallBinding;
import com.tangrun.mschat.databinding.MsItemBuddyBinding;
import com.tangrun.mschat.databinding.MsLayoutActionBinding;
import com.tangrun.mschat.model.BuddyModel;
import com.tangrun.mschat.model.IBuddyModelObserver;
import com.tangrun.mschat.model.UIRoomStore;
import com.tangrun.mslib.enums.CameraFacingState;
import com.tangrun.mslib.enums.ConnectionState;
import com.tangrun.mslib.enums.ConversationState;
import com.tangrun.mslib.enums.LocalConnectState;
import com.tangrun.mslib.lv.ChangedMutableLiveData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/13 21:35
 */
public class MultiCallRoomFragment extends Fragment {

    private static final String TAG = "MS_MultiFragment";

    public static MultiCallRoomFragment newInstance() {

        Bundle args = new Bundle();

        MultiCallRoomFragment fragment = new MultiCallRoomFragment();
        fragment.setArguments(args);
        return fragment;
    }


    MsFragmentMultiCallBinding binding;
    UIRoomStore uiRoomStore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = MsFragmentMultiCallBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        // 初始化
        uiRoomStore = MSManager.getCurrent();

        // 列表
        MultiAdapter adapter = new MultiAdapter(uiRoomStore, this);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        binding.msRvBuddys.setLayoutManager(layoutManager);
        binding.msRvBuddys.setAdapter(adapter);
        // 数量变化设置列数
        {
            ChangedMutableLiveData<Integer> itemCount = new ChangedMutableLiveData<>(0);
            RecyclerView.AdapterDataObserver adapterDataObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    int size = adapter.getItemCount();
                    itemCount.applySet(size);

                }
            };
            itemCount.observe(this, integer -> {
                layoutManager.setSpanCount(integer < 5 ? 2 : integer < 10 ? 3 : 4);
            });
//            adapter.registerAdapterDataObserver(adapterDataObserver);
//            getLifecycle().addObserver(new LifecycleEventObserver() {
//                @Override
//                public void onStateChanged(@NonNull @NotNull LifecycleOwner source, @NonNull @NotNull Lifecycle.Event event) {
//                    if (event == Lifecycle.Event.ON_DESTROY) {
//                        source.getLifecycle().removeObserver(this);
//                        adapter.unregisterAdapterDataObserver(adapterDataObserver);
//                    }
//                }
//            });
            uiRoomStore.buddyObservable.registerObserver(new IBuddyModelObserver() {
                @Override
                public void onBuddyAdd(int position, BuddyModel buddyModel) {
                    adapter.notifyItemInserted(position);
                    adapterDataObserver.onChanged();
                }

                @Override
                public void onBuddyRemove(int position, BuddyModel buddyModel) {
                    adapter.notifyItemRemoved(position);
                    adapterDataObserver.onChanged();
                }

            });
        }
        adapter.setList(uiRoomStore.buddyModels);
        // 镜像设置
        uiRoomStore.cameraFacingState.observe(this, cameraFacingState -> {
            if (cameraFacingState == CameraFacingState.inProgress) return;
            int index = adapter.list.indexOf(uiRoomStore.mine.getValue());
            if (index < 0) return;
            adapter.notifyItemChanged(index);
        });

        uiRoomStore.callTime.observe(this, s -> {
            binding.msTvTitle.setVisibility(s == null ? View.GONE : View.VISIBLE);
            binding.msTvTitle.setText(s);
        });
        uiRoomStore.localState.observe(this, localState -> {
            setTipText();

            hideAllAction();
            if (localState.second == ConversationState.Invited) {
                // 接听/挂断
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomLeft);
                uiRoomStore.Action_JoinAction.bindView(binding.llActionBottomRight);
            } else if (localState.second == ConversationState.Joined
                    || (localState.second == ConversationState.New && localState.first == LocalConnectState.JOINED)) {
                if (uiRoomStore.audioOnly) {
                    // 麦克风/挂断/扬声器
                    uiRoomStore.Action_SpeakerOn.bindView(binding.llActionBottomLeft);
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                    uiRoomStore.Action_MicrophoneDisabled.bindView(binding.llActionBottomRight);
                } else {
                    // 麦克风/摄像头/切换摄像头 挂断
                    uiRoomStore.Action_SpeakerOn.bindView(binding.llActionTopLeft);
                    uiRoomStore.Action_CameraDisabled.bindView(binding.llActionTopCenter);
                    uiRoomStore.Action_CameraNotFacing.bindView(binding.llActionTopRight);
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                }
            } else {
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
            }
        });
        // 点击事件
        binding.msIvAdd.setOnClickListener(v -> {
            uiRoomStore.onAddUserClick(getContext());
        });
        binding.msIvMinimize.setOnClickListener(v -> {
            uiRoomStore.onMinimize(getActivity());
        });

    }

    Runnable tipDismiss = () -> {
        binding.msTvTip.setVisibility(View.GONE);
    };

    private void setTipText() {
        String tip = null;
        int delayDismissTime = 0;
        binding.msTvTip.removeCallbacks(tipDismiss);

        Pair<LocalConnectState, ConversationState> localState = uiRoomStore.localState.getValue();

        if (localState != null) {
            if (localState.first == LocalConnectState.NEW || localState.first == LocalConnectState.CONNECTING) {
                tip = "连接中...";
            } else if (localState.first == LocalConnectState.DISCONNECTED || localState.first == LocalConnectState.RECONNECTING) {
                tip = "重连中...";
            } else {
                if (localState.second == ConversationState.New) {
                    tip = "等待对方接听...";
                } else if (localState.second == ConversationState.Invited) {
                    tip = "待接听";
                } else if (localState.second == ConversationState.Joined) {
                    tip = "通话中...";
                    delayDismissTime = 2000;
                } else if (localState.second == ConversationState.InviteBusy
                        || localState.second == ConversationState.Left
                        || localState.second == ConversationState.InviteReject
                        || localState.second == ConversationState.OfflineTimeout
                        || localState.second == ConversationState.InviteTimeout) {
                    tip = "通话已结束";
                }
            }
        }

        if (tip != null) {
            binding.msTvTip.setText(tip);
            binding.msTvTip.setVisibility(View.VISIBLE);
        }
        if (delayDismissTime > 0)
            binding.msTvTip.postDelayed(tipDismiss, delayDismissTime);
    }

    public void hideAllAction() {
        for (MsLayoutActionBinding itemActionBinding : Arrays.asList(
                binding.llActionTopLeft,
                binding.llActionTopRight,
                binding.llActionTopCenter,
                binding.llActionBottomLeft,
                binding.llActionBottomRight,
                binding.llActionBottomCenter)) {
            itemActionBinding.llContent.setVisibility(View.INVISIBLE);
        }
    }

    public static class MultiAdapter extends RecyclerView.Adapter<MultiAdapter.ViewHolder<MsItemBuddyBinding>> {

        List<BuddyModel> list = new ArrayList<>();
        UIRoomStore uiRoomStore;
        LifecycleOwner lifecycleOwner;

        public MultiAdapter(UIRoomStore uiRoomStore, LifecycleOwner lifecycleOwner) {
            this.uiRoomStore = uiRoomStore;
            this.lifecycleOwner = lifecycleOwner;
        }

        @NonNull
        @Override
        public ViewHolder<MsItemBuddyBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MsItemBuddyBinding binding = MsItemBuddyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder<>(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder<MsItemBuddyBinding> holder, int position) {
            MsItemBuddyBinding binding = holder.binding;
            BuddyModel model = list.get(position);

            binding.tvDisplayName.setText(model.buddy.getDisplayName());
            Glide.with(binding.ivCover).load(model.buddy.getAvatar())
                    .apply(new RequestOptions()
                            .error(R.drawable.ms_default_avatar)
                            .placeholder(R.drawable.ms_default_avatar))
                    .into(binding.ivCover);

            model.videoTrack.removeObservers(lifecycleOwner);
            model.videoTrack.observe(lifecycleOwner, videoTrack -> {
                Log.d(TAG, "onBindViewHolder: " + model.buddy.getDisplayName() + " videoTrack = " + videoTrack);
                binding.vRenderer.setVisibility(videoTrack == null ? View.GONE : View.VISIBLE);
                binding.ivCover.setVisibility(videoTrack != null ? View.GONE : View.VISIBLE);
                binding.vRenderer.init(lifecycleOwner);
                binding.vRenderer.bind(lifecycleOwner, uiRoomStore.callingActual.getValue() == Boolean.TRUE, videoTrack);
                binding.vRenderer.setMirror(model.buddy.isProducer() && videoTrack != null && uiRoomStore.cameraFacingState.getValue() == CameraFacingState.front);
            });

            model.disabledMic.removeObservers(lifecycleOwner);
            model.disabledMic.observe(lifecycleOwner, aBoolean -> {
                binding.ivMicDisable.setVisibility(aBoolean != null && aBoolean ? View.VISIBLE : View.GONE);
            });

            model.disabledCam.removeObservers(lifecycleOwner);
            model.disabledCam.observe(lifecycleOwner, aBoolean -> {
                binding.ivCamDisable.setVisibility(aBoolean != null && aBoolean ? View.VISIBLE : View.GONE);
            });

            model.volume.removeObservers(lifecycleOwner);
            model.volume.observe(lifecycleOwner, integer -> {
                binding.ivVoiceOn.setVisibility(integer == null || integer == 0 ? View.GONE : View.VISIBLE);
            });

            if (!model.buddy.isProducer()) {
                model.state.removeObservers(lifecycleOwner);
                model.state.observe(lifecycleOwner, value -> {
                    String text = null;

                    switch (model.connectionState.getValue() == null ? ConnectionState.NEW : model.connectionState.getValue()) {
                        case Offline: {
                            text = "断线重连中...";
                            break;
                        }
                        case Left: {
                            text = "已离开";
                            break;
                        }
                        default: {
                            if (model.conversationState.getValue() == null) break;
                            switch (model.conversationState.getValue()) {
                                case Invited: {
                                    text = "等待接听...";
                                    break;
                                }
                                case InviteBusy: {
                                    text = "对方忙线";
                                    break;
                                }
                                case InviteTimeout: {
                                    text = "无人接听";
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    binding.msTvTip.setVisibility(text == null ? View.GONE : View.VISIBLE);
                    if (text != null)
                        binding.msTvTip.setText(text);
                });
            }

//            Runnable stateRunnable = () -> {
//                binding.tvDebug.setText(model.connectionState.getValue() +
//                        " " + model.conversationState.getValue() +
//                        " A(p" + model.audioPScore.getValue() + " c" + model.audioCScore.getValue() + ")" +
//                        " V(p" + model.videoPScore.getValue() + " c" + model.videoCScore.getValue() + ")"
//                );
//            };
//
//            model.connectionState.observe(lifecycleOwner, value -> {
//                stateRunnable.run();
//            });
//            model.audioPScore.observe(lifecycleOwner, value -> {
//                stateRunnable.run();
//            });
//            model.audioCScore.observe(lifecycleOwner, value -> {
//                stateRunnable.run();
//            });
//            model.videoPScore.observe(lifecycleOwner, value -> {
//                stateRunnable.run();
//            });
//            model.videoCScore.observe(lifecycleOwner, value -> {
//                stateRunnable.run();
//            });

        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void setList(List<BuddyModel> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        public static class ViewHolder<T extends ViewBinding> extends RecyclerView.ViewHolder {
            T binding;

            public ViewHolder(T binding) {
                this(binding.getRoot());
                this.binding = binding;
            }

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

}
