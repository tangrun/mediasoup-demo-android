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

        RoomViewModel roomViewModel = ViewModelProviders.of(this).get(RoomViewModel.class);

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
                roomState.getConnectionState()
            }
        });

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

    public static class MultiData extends BaseObservable {

        private ObservableField<Action> left = new ObservableField<>();
        private ObservableField<Action> right = new ObservableField<>();
        private ObservableField<Action> center = new ObservableField<>();
        private ObservableField<Action> bottom = new ObservableField<>();
        private ObservableField<RoomStore> roomStore = new ObservableField<>();
        private ObservableField<RoomInfo> roomInfoObservableField = new ObservableField<>();
        private ObservableField<Peers> peersObservableField = new ObservableField<>();


        public void onMin() {

        }

        public void onAdd() {

        }


        public ObservableField<Action> getLeft() {
            return left;
        }

        public ObservableField<Action> getRight() {
            return right;
        }

        public ObservableField<Action> getCenter() {
            return center;
        }

        public ObservableField<Action> getBottom() {
            return bottom;
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
