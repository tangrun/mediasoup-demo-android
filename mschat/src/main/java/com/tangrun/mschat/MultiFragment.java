package com.tangrun.mschat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mschat.R;

import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/13 21:35
 */
public class MultiFragment extends Fragment {
    private AppCompatImageView ivMin;
    private AppCompatTextView tvTitle;
    private AppCompatImageView ivAdd;
    private RecyclerView remotePeers;
    private LinearLayout llAction;

    public static class MultiAdapter extends RecyclerView.Adapter<MultiAdapter.ViewHolder> {

        List<Object> list = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }


        public static class ViewHolder extends RecyclerView.ViewHolder {
            private SurfaceViewRenderer vRenderer;
            private AppCompatImageView ivCover;
            private AppCompatTextView tvDisplayName;
            private AppCompatImageView ivMicDisable;
            private AppCompatImageView ivCamDisable;
            private AppCompatImageView ivVoiceOn;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                initView(this, itemView);
            }

            private void initView(ViewHolder viewHolder, View convertView) {
                viewHolder.vRenderer = (SurfaceViewRenderer) convertView.findViewById(R.id.v_renderer);
                viewHolder.ivCover = (AppCompatImageView) convertView.findViewById(R.id.iv_cover);
                viewHolder.tvDisplayName = (AppCompatTextView) convertView.findViewById(R.id.tv_display_name);
                viewHolder.ivMicDisable = (AppCompatImageView) convertView.findViewById(R.id.iv_mic_disable);
                viewHolder.ivCamDisable = (AppCompatImageView) convertView.findViewById(R.id.iv_cam_disable);
                viewHolder.ivVoiceOn = (AppCompatImageView) convertView.findViewById(R.id.iv_voice_on);
            }
        }
    }

    @Nullable
    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room_multi, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();

    }

    protected <T> T findViewById(int id) {
        return (T) getView().findViewById(id);
    }

    private void initView() {
        ivMin = (AppCompatImageView) findViewById(R.id.iv_min);
        tvTitle = (AppCompatTextView) findViewById(R.id.tv_title);
        ivAdd = (AppCompatImageView) findViewById(R.id.iv_add);
        remotePeers = (RecyclerView) findViewById(R.id.remote_peers);
        llAction = (LinearLayout) findViewById(R.id.ll_action);
    }
}
