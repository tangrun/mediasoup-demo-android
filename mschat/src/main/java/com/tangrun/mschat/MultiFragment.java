package com.tangrun.mschat;

import android.app.Application;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableField;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mschat.R;
import com.example.mschat.databinding.FragmentRoomMultiBinding;
import com.example.mschat.databinding.ItemPeerBinding;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Buddy;
import org.mediasoup.droid.lib.model.Buddys;
import org.mediasoup.droid.lib.model.Peers;
import org.mediasoup.droid.lib.model.RoomInfo;
import org.mediasoup.droid.lib.model.RoomState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/13 21:35
 */
public class MultiFragment extends Fragment {

    public static MultiFragment newInstance() {

        Bundle args = new Bundle();

        MultiFragment fragment = new MultiFragment();
        fragment.setArguments(args);
        return fragment;
    }


    FragmentRoomMultiBinding binding;


    @Nullable
    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_room_multi, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RoomViewModel roomViewModel = ViewModelProviders.of(getActivity()).get(RoomViewModel.class);
        binding.setData(roomViewModel);
        MultiAdapter adapter = new MultiAdapter(roomViewModel.getRoomClient(),this);
        binding.rvBuddys.setAdapter(adapter);
        GridLayoutManager layout = new GridLayoutManager(getContext(), 2);
        binding.rvBuddys.setLayoutManager(layout);
        roomViewModel.getRoomStore().getBuddys().observe(this, new Observer<Buddys>() {
            @Override
            public void onChanged(Buddys buddys) {
                adapter.setList(buddys.getAllPeers());
            }
        });
        roomViewModel.getRoomStore().getRoomState().observe(this, new Observer<RoomState>() {
            @Override
            public void onChanged(RoomState roomState) {
                roomViewModel.setConnectionState(roomState.getConnectionState());
            }
        });

        roomViewModel.setBottom(new Action("挂断", R.drawable.selector_call_hangup, new Runnable() {
            @Override
            public void run() {
                roomViewModel.getRoomClient().hangup();
            }
        }));
        roomViewModel.setLeft(new Action("麦克风", R.drawable.selector_call_mute, new Runnable() {
            @Override
            public void run() {
                roomViewModel.onMic();
            }
        }));
        roomViewModel.setCenter(new Action("扬声器", R.drawable.selector_call_speaker, new Runnable() {
            @Override
            public void run() {

            }
        }));
        roomViewModel.setRight(new Action("摄像头", R.drawable.selector_call_enable_camera, new Runnable() {
            @Override
            public void run() {
                roomViewModel.onCam();
            }
        }));

    }


    public static class MultiAdapter extends RecyclerView.Adapter<MultiAdapter.ViewHolder<ItemPeerBinding>> {

        List<Buddy> list = new ArrayList<>();
        RoomClient roomClient;
        LifecycleOwner lifecycleOwner;

        public MultiAdapter(RoomClient roomClient, LifecycleOwner lifecycleOwner) {
            this.roomClient = roomClient;
            this.lifecycleOwner = lifecycleOwner;
        }

        @NonNull
        @Override
        public ViewHolder<ItemPeerBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemPeerBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_peer, parent, false);
            return new ViewHolder<>(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder<ItemPeerBinding> holder, int position) {
            BuddyItemViewModel model = new BuddyItemViewModel((Application) holder.itemView.getContext().getApplicationContext());
            model.setRoomStore(roomClient);
            holder.binding.setData(model);
            model.connect(lifecycleOwner,list.get(position).getId());
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void setList(List<Buddy> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        public static class ViewHolder<T extends ViewDataBinding> extends RecyclerView.ViewHolder {
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


    public static class Action implements Runnable {
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

        @Override
        public void run() {

        }
    }
}
