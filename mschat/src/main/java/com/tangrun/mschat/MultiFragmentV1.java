package com.tangrun.mschat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.mschat.R;
import com.example.mschat.databinding.FragmentRoomMultiBinding;
import com.example.mschat.databinding.ItemActionBinding;
import com.example.mschat.databinding.ItemPeerBinding;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.MultiMutableLiveData;
import org.mediasoup.droid.lib.model.Buddy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/13 21:35
 */
public class MultiFragmentV1 extends Fragment {

    private static final String TAG = "MS_MultiFragment";

    public static MultiFragmentV1 newInstance() {

        Bundle args = new Bundle();

        MultiFragmentV1 fragment = new MultiFragmentV1();
        fragment.setArguments(args);
        return fragment;
    }


    FragmentRoomMultiBinding binding;


    @Nullable
    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRoomMultiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // 初始化
        UIRoomStore uiRoomStore = MSManager.getCurrent();
        uiRoomStore.bindLifeOwner(this);

        // 列表
        MultiAdapter adapter = new MultiAdapter(uiRoomStore.getRoomClient(), this);
        binding.rvBuddys.setAdapter(adapter);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        binding.rvBuddys.setLayoutManager(layoutManager);

        // 数据变化页面显示
        uiRoomStore.buddys.observe(this, buddyItemViewModels -> {
            adapter.setList(buddyItemViewModels);
            int size = buddyItemViewModels.size();
            layoutManager.setSpanCount(size < 5 ? 2 : size < 10 ? 3 : 4);
        });
        uiRoomStore.callTime.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                if (aLong == null) {
                    binding.tvTitle.setVisibility(View.INVISIBLE);
                    return;
                }
                binding.tvTitle.setVisibility(View.VISIBLE);
                long minute = aLong / 1000 / 60;
                long second = aLong / 1000 - minute * 60;
                binding.tvTitle.setText(String.format("%d:%d", minute, second));
            }
        });
        uiRoomStore.conversationState.observe(this, conversationState -> {
            hideAllAction();
            if (conversationState == Buddy.ConversationState.Invited) {
                // 接听/挂断
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomLeft);
                uiRoomStore.Action_JoinAction.bindView(binding.llActionBottomRight);
            } else if (conversationState == Buddy.ConversationState.Joined) {
                if (uiRoomStore.audioOnly) {
                    // 麦克风/挂断/扬声器
                    uiRoomStore.Action_MicDisabledAction.bindView(binding.llActionBottomLeft);
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                    uiRoomStore.Action_SpeakerOnAction.bindView(binding.llActionBottomRight);
                } else {
                    // 麦克风/摄像头/切换摄像头 挂断
                    uiRoomStore.Action_MicDisabledAction.bindView(binding.llActionTopLeft);
                    uiRoomStore.Action_CamDisabledAction.bindView(binding.llActionTopCenter);
                    uiRoomStore.Action_CamNotIsFrontAction.bindView(binding.llActionTopRight);
                    uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
                }
            } else {
                uiRoomStore.Action_HangupAction.bindView(binding.llActionBottomCenter);
            }
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
        for (ItemActionBinding itemActionBinding : Arrays.asList(
                binding.llActionTopLeft,
                binding.llActionTopRight,
                binding.llActionTopCenter,
                binding.llActionBottomLeft,
                binding.llActionBottomRight,
                binding.llActionBottomCenter)) {
            itemActionBinding.llContent.setVisibility(View.INVISIBLE);
        }
    }

    public static class MultiAdapter extends RecyclerView.Adapter<MultiAdapter.ViewHolder<ItemPeerBinding>> {

        List<BuddyItemViewModel> list = new ArrayList<>();
        RoomClient roomClient;
        LifecycleOwner lifecycleOwner;

        public MultiAdapter(RoomClient roomClient, LifecycleOwner lifecycleOwner) {
            this.roomClient = roomClient;
            this.lifecycleOwner = lifecycleOwner;
        }

        @NonNull
        @Override
        public ViewHolder<ItemPeerBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemPeerBinding binding = ItemPeerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder<>(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder<ItemPeerBinding> holder, int position) {
            ItemPeerBinding binding = holder.binding;
            BuddyItemViewModel model = list.get(position);

            binding.tvDisplayName.setText(model.buddy.getDisplayName());
            Glide.with(binding.ivCover).load(model.buddy.getAvatar())
                    .apply(new RequestOptions()
                            .error(R.drawable.buddy)
                            .placeholder(R.drawable.buddy))
                    .into(binding.ivCover);

            model.mVideoTrack.observe(lifecycleOwner, videoTrack -> {
                binding.vRenderer.setVisibility(videoTrack == null ? View.GONE : View.VISIBLE);
                binding.ivCover.setVisibility(videoTrack != null ? View.GONE : View.VISIBLE);
                binding.vRenderer.init(lifecycleOwner);
                binding.vRenderer.bind(lifecycleOwner, videoTrack);
            });
            model.mDisabledMic.observe(lifecycleOwner, aBoolean -> {
                binding.ivMicDisable.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
            });
            model.mDisabledCam.observe(lifecycleOwner, aBoolean -> {
                binding.ivCamDisable.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
            });
            model.mVolume.observe(lifecycleOwner, integer -> {
                binding.ivVoiceOn.setVisibility(integer == null ? View.GONE : View.VISIBLE);
            });

            Runnable stateRunnable = () -> {
                binding.tvDebug.setText(model.connectionState.getValue() +
                        " " + model.conversationState.getValue() +
                        " A(p" + model.mAudioPScore.getValue() + " c" + model.mAudioCScore.getValue() + ")" +
                        " V(p" + model.mVideoPScore.getValue() + " c" + model.mVideoCScore.getValue() + ")"
                );
            };
            MultiMutableLiveData multiMutableLiveData = new MultiMutableLiveData();
            multiMutableLiveData.addSource(model.connectionState);
            multiMutableLiveData.addSource(model.conversationState);
            multiMutableLiveData.observe(lifecycleOwner, value -> {
                String text = null;

                switch (model.connectionState.getValue() == null ? Buddy.ConnectionState.NEW : model.connectionState.getValue()) {
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
            model.connectionState.observe(lifecycleOwner, value -> {
                stateRunnable.run();
            });
            model.mAudioPScore.observe(lifecycleOwner, value -> {
                stateRunnable.run();
            });
            model.mAudioCScore.observe(lifecycleOwner, value -> {
                stateRunnable.run();
            });
            model.mVideoPScore.observe(lifecycleOwner, value -> {
                stateRunnable.run();
            });
            model.mVideoCScore.observe(lifecycleOwner, value -> {
                stateRunnable.run();
            });

        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void setList(List<BuddyItemViewModel> list) {
            this.list.clear();
            this.list.addAll(list);
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
