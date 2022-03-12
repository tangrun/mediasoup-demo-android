package com.example.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SelectUserActivity extends AppCompatActivity {

    private RecyclerView rvContent;

    public static class User extends com.tangrun.mschat.model.User {
        boolean checked;
    }

    private static class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        List<User> list = new ArrayList<>();

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            User user = list.get(position);
            Glide.with(holder.ivAvatar)
                    .load(user.getAvatar())
                    .into(holder.ivAvatar);
            holder.tvId.setText(user.getId());
            holder.tvName.setText(user.getDisplayName());
            holder.cbCheck.setChecked(user.checked);
            holder.itemView.setOnClickListener(v -> {
                user.checked = !user.checked;
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }



        public class Holder extends RecyclerView.ViewHolder {
            private AppCompatImageView ivAvatar;
            private AppCompatTextView tvId;
            private AppCompatTextView tvName;
            private AppCompatCheckBox cbCheck;
            public Holder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = (AppCompatImageView) itemView.findViewById(R.id.iv_avatar);
                tvId = (AppCompatTextView) itemView.findViewById(R.id.tv_id);
                tvName = (AppCompatTextView) itemView.findViewById(R.id.tv_name);
                cbCheck = (AppCompatCheckBox) itemView.findViewById(R.id.cb_check);
            }
        }

    }

    Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user);
        initView();
        adapter = new Adapter();
        rvContent.setAdapter(adapter);
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        ArrayList<User> list = new ArrayList<>();
        int i =0;
        for (String icon : MainActivity.icons) {
            i++;
            User user = new User();
            user.setId("100"+i);
            user.setDisplayName("user "+i);
            user.setAvatar(icon);
            list.add(user);
        }
        adapter.list.addAll(list);
        adapter.notifyDataSetChanged();
    }

    public void onSure(View view) {
        Intent data = new Intent();
        ArrayList<User> list = new ArrayList<>();
        for (User user : adapter.list) {
            if (user.checked)list.add(user);
        }
        if (list.isEmpty()){
            Toast.makeText(this, "没有选中", Toast.LENGTH_SHORT).show();
            return;
        }
        data.putExtra("data", list);
        setResult(RESULT_OK, data);
        finish();
    }

    public void onBack(View view) {
        onBackPressed();
    }

    private void initView() {
        rvContent = (RecyclerView) findViewById(R.id.rv_content);
    }
}