package com.tangrun.mschat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.mschat.R;

import com.example.mschat.databinding.MsFragmentMultiCallBinding;
import com.example.mschat.databinding.MsItemBuddyBinding;
import com.example.mschat.databinding.MsLayoutActionBinding;
import com.tangrun.mschat.model.BuddyModel;
import com.tangrun.mschat.model.IBuddyModelObserver;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.model.UIRoomStore;
import org.jetbrains.annotations.NotNull;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.enums.ConnectionState;
import org.mediasoup.droid.lib.enums.ConversationState;
import org.mediasoup.droid.lib.lv.ChangedMutableLiveData;
import org.mediasoup.droid.lib.lv.MultiMutableLiveData;

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


    @Nullable
    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = MsFragmentMultiCallBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // 初始化
        UIRoomStore uiRoomStore = MSManager.getCurrent();

        // 列表
        MultiAdapter adapter = new MultiAdapter(uiRoomStore.getRoomClient(), this);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        binding.rvBuddys.setLayoutManager(layoutManager);
        binding.rvBuddys.setAdapter(adapter);
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
            adapter.registerAdapterDataObserver(adapterDataObserver);
            getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull @NotNull LifecycleOwner source, @NonNull @NotNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        source.getLifecycle().removeObserver(this);
                        adapter.unregisterAdapterDataObserver(adapterDataObserver);
                    }
                }
            });
        }
        uiRoomStore.buddyObservable.registerObserver(new IBuddyModelObserver() {
            @Override
            public void onBuddyAdd(int position, BuddyModel buddyModel) {
                adapter.notifyItemInserted(position);
            }

            @Override
            public void onBuddyRemove(int position, BuddyModel buddyModel) {
                adapter.notifyItemRemoved(position);
            }

        });
        adapter.setList(uiRoomStore.buddyModels);


        uiRoomStore.callTime.observe(this, s -> {
            binding.tvTitle.setVisibility(s == null ? View.GONE : View.VISIBLE);
            binding.tvTitle.setText(s);
        });
        uiRoomStore.localConversationState.observe(this, conversationState -> {
            hideAllAction();
            if (conversationState == ConversationState.Invited) {
                // 接听/挂断
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomLeft);
                uiRoomStore.Action_JoinAction.bindView(binding.llActionBottomRight);
            } else if (conversationState == ConversationState.Joined) {
                if (uiRoomStore.audioOnly) {
                    // 麦克风/挂断/扬声器
                    uiRoomStore.Action_MicrophoneDisabled.bindView(binding.llActionBottomLeft);
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                    uiRoomStore.Action_SpeakerOn.bindView(binding.llActionBottomRight);
                } else {
                    // 麦克风/摄像头/切换摄像头 挂断
                    uiRoomStore.Action_MicrophoneDisabled.bindView(binding.llActionTopLeft);
                    uiRoomStore.Action_CameraDisabled.bindView(binding.llActionTopCenter);
                    uiRoomStore.Action_CameraNotFacing.bindView(binding.llActionTopRight);
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                }
            } else {
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
            }
        });
        uiRoomStore.localConnectionState.observe(this, localConnectState -> {
            String text = null;
            switch (localConnectState) {
                case CLOSED: {
                    text = "通话已结束";
                    break;
                }
                case NEW:
                case CONNECTING: {
                    text = "连接中...";
                    break;
                }
                case RECONNECTING:
                case DISCONNECTED: {
                    text = "连接中，请稍等...";
                    break;
                }
            }
            binding.tvTip.setVisibility(text == null ? View.GONE : View.VISIBLE);
            if (text != null)
                binding.tvTip.setText(text);
        });
        // 点击事件
        binding.ivAdd.setOnClickListener(v -> {
            uiRoomStore.onAddUserClick(getContext());
        });
        binding.ivMin.setOnClickListener(v -> {
            uiRoomStore.onMinimize(getActivity());
        });

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
        RoomClient roomClient;
        LifecycleOwner lifecycleOwner;

        public MultiAdapter(RoomClient roomClient, LifecycleOwner lifecycleOwner) {
            this.roomClient = roomClient;
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
                            .error(R.drawable.buddy)
                            .placeholder(R.drawable.buddy))
                    .into(binding.ivCover);

            model.videoTrack.removeObservers(lifecycleOwner);
            model.videoTrack.observe(lifecycleOwner, videoTrack -> {
                binding.vRenderer.setVisibility(videoTrack == null ? View.GONE : View.VISIBLE);
                binding.ivCover.setVisibility(videoTrack != null ? View.GONE : View.VISIBLE);
                binding.vRenderer.init(lifecycleOwner);
                binding.vRenderer.bind(lifecycleOwner, videoTrack);
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

                    binding.tvTip.setVisibility(text == null ? View.GONE : View.VISIBLE);
                    if (text != null)
                        binding.tvTip.setText(text);
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
