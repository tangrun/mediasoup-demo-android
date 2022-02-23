package com.tangrun.mschat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
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
import org.mediasoup.droid.lib.model.RoomState;

import java.util.ArrayList;
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
        UIRoomStore uiRoomStore = MSManager.getCurrent();
        MultiAdapter adapter = new MultiAdapter(uiRoomStore.getRoomClient(), this);
        binding.rvBuddys.setAdapter(adapter);
        GridLayoutManager layout = new GridLayoutManager(getContext(), 2);
        binding.rvBuddys.setLayoutManager(layout);
        uiRoomStore.buddys.observe(this, buddyItemViewModels -> {
            adapter.setList(buddyItemViewModels);
        });
        uiRoomStore.callTime.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                if (aLong == null){
                    binding.tvTitle.setVisibility(View.GONE);
                    return;
                }
                binding.tvTitle.setVisibility(View.VISIBLE);
                long minute = aLong / 1000 / 60;
                long second = aLong / 1000 - minute * 60;
                binding.tvTitle.setText(String.format("%d:%d", minute, second));
            }
        });
        uiRoomStore.conversationState.observe(this, conversationState -> {

        });

        uiRoomStore.connectionState.observe(this, connectionState -> {
            binding.tvTitle.setText(connectionState.toString());
        });
        uiRoomStore.camState.observe(this, state -> {
            binding.llActionTopRight.ivImg.setSelected(state == RoomState.State.Off);
        });
        uiRoomStore.micState.observe(this, state -> {
            binding.llActionTopLeft.ivImg.setSelected(state == RoomState.State.Off);
        });
        uiRoomStore.speakerState.observe(this, state -> {
            binding.llActionBottomLeft.ivImg.setSelected(state == RoomState.State.On);
        });
        uiRoomStore.switchCamState.observe(this, state -> {
            binding.llActionBottomRight.ivImg.setSelected(state == RoomState.State.Off);
        });
        setAction(binding.llActionTopLeft, "麦克风", R.drawable.selector_call_mute, v -> {
            uiRoomStore.switchMicEnable();
        });
        setAction(binding.llActionBottomLeft, "扬声器", R.drawable.selector_call_speaker, v -> {
            uiRoomStore.switchSpeakerphoneEnable();
        });
        setAction(binding.llActionTopCenter, "接听", R.drawable.selector_call_audio_answer, v -> {
            uiRoomStore.join();
        });
        setAction(binding.llActionBottomCenter, "挂断", R.drawable.selector_call_hangup, v -> {
            uiRoomStore.hangup();
            binding.llActionBottomCenter.ivImg.postDelayed(() -> {
                MSManager.stopCall();
                getActivity().finish();
            }, 1000);
        });
        setAction(binding.llActionTopRight, "摄像头", R.drawable.selector_call_enable_camera, v -> {
            uiRoomStore.switchCamEnable();
        });
        setAction(binding.llActionBottomRight, "切换", R.drawable.selector_call_switch_camera, v -> {
            uiRoomStore.switchCamDevice();
        });
        binding.ivAdd.setOnClickListener(v -> {
            uiRoomStore.onAddUserClick(getContext());
        });

    }

    public void setAction(ItemActionBinding binding, String text, int id, View.OnClickListener v) {
        binding.llContent.setVisibility(View.VISIBLE);
        binding.llContent.setOnClickListener(v);
        binding.tvContent.setText(text);
        binding.ivImg.setImageResource(id);
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
                if (videoTrack != null) {
                    binding.vRenderer.init();
                    videoTrack.addSink(binding.vRenderer);
                } else {
                    binding.vRenderer.clear();
                }
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


            Observer<Integer> scoreObserver = integer -> {
                if (integer != null && Integer.valueOf(0).equals(integer)) {
                    binding.tvTip.setText("网络连接中...");
                } else {
                    binding.tvTip.setText("");
                }
            };
            model.mAudioPScore.observe(lifecycleOwner, scoreObserver);
            model.mVideoPScore.observe(lifecycleOwner, scoreObserver);


            model.mStateTip.observe(lifecycleOwner, s -> {
                binding.tvTip.setText(s);
            });


            Runnable stateRunnable = () -> {
                binding.tvDebug.setText(model.connectionState.getValue() +
                        " " + model.conversationState.getValue() +
                        " A(p" + model.mAudioPScore.getValue() + " c" + model.mAudioCScore.getValue() + ")" +
                        " V(p" + model.mVideoPScore.getValue() + " c" + model.mVideoCScore.getValue() + ")"
                );
            };
            model.conversationState.observe(lifecycleOwner, value -> {
                stateRunnable.run();
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


    public static class Action {
        String name;
        int imgId;
        boolean checked;
        boolean enabled;
        Runnable click;

        public Action(String name, int imgId, Runnable click) {
            this.name = name;
            this.imgId = imgId;
            this.click = click;
        }

        public Runnable getClick() {
            return click;
        }

        public Action setClick(Runnable click) {
            this.click = click;
            return this;
        }

        public String getName() {
            return name;
        }

        public Action setName(String name) {
            this.name = name;
            return this;
        }

        public int getImgId() {
            return imgId;
        }

        public Action setImgId(int imgId) {
            this.imgId = imgId;
            return this;
        }

        public boolean isChecked() {
            return checked;
        }

        public Action setChecked(boolean checked) {
            this.checked = checked;
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Action setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
    }
}
