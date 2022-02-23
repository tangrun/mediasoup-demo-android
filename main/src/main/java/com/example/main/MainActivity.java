package com.example.main;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;

import com.tangrun.mschat.Start;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private AppCompatEditText tvId;
    private AppCompatEditText tvName;
    private AppCompatButton btStart;

    String[] icons = {
            "https://img0.baidu.com/it/u=1056811702,4111096278&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
            "https://img1.baidu.com/it/u=2716398045,2043787292&fm=253&fmt=auto&app=120&f=JPEG?w=800&h=800",
            "https://img2.baidu.com/it/u=1437480680,2169625508&fm=253&fmt=auto&app=138&f=JPEG?w=360&h=360",
            "https://img0.baidu.com/it/u=2672781059,3251606716&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
            "https://img2.baidu.com/it/u=2090606195,1473750087&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
            "https://img1.baidu.com/it/u=3249070913,912844529&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
            "https://img1.baidu.com/it/u=793269991,2224346596&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
            "https://img2.baidu.com/it/u=1617362238,740727550&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
            "https://img1.baidu.com/it/u=2929809463,2042799416&fm=253&fmt=auto&app=120&f=JPEG?w=690&h=690",
    };
    private AppCompatCheckBox cbAudioOnly;
    private AppCompatEditText tvRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        SharedPreferences aaa = getSharedPreferences("aaa", 0);
        tvId.setText(aaa.getString("tvId", ""));
        tvName.setText(aaa.getString("tvName", ""));
        tvRoom.setText(aaa.getString("tvRoom", ""));
        cbAudioOnly.setChecked(aaa.getBoolean("cbAudioOnly", false));
        btStart.setOnClickListener(v -> {
            aaa.edit()
                    .putString("tvId", tvId.getText().toString())
                    .putString("tvName", tvName.getText().toString())
                    .putString("tvRoom", tvRoom.getText().toString())
                    .putBoolean("cbAudioOnly", cbAudioOnly.isChecked())
                    .apply();
            Start.User user = new Start.User();
            user.setId(tvId.getText().toString());
            user.setDisplayName(tvName.getText().toString());
            user.setAvatar(icons[new Random().nextInt(icons.length)]);
            Start.startMultiCall(this, tvRoom.getText().toString(), user, cbAudioOnly.isChecked(), null);
        });
    }

    private void initView() {
        tvId = (AppCompatEditText) findViewById(R.id.tv_id);
        tvName = (AppCompatEditText) findViewById(R.id.tv_name);
        btStart = (AppCompatButton) findViewById(R.id.bt_start);
        cbAudioOnly = findViewById(R.id.cb_audioOnly);
        tvRoom = findViewById(R.id.tv_room);
    }
}