package com.example.main;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import com.tangrun.mschat.MSManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private AppCompatEditText tvId;
    private AppCompatEditText tvName;
    private AppCompatButton btStart;

    public static String[] icons = {
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
    List<MSManager.User> userList = new ArrayList<>();
    private AppCompatTextView tvUser;
    private AppCompatButton btSelect;
    private AppCompatCheckBox cbMulti;
    private AppCompatButton btJoin;
    SharedPreferences aaa;
    private AppCompatButton btBusy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        aaa = getSharedPreferences("aaa", 0);
        tvId.setText(aaa.getString("tvId", ""));
        tvName.setText(aaa.getString("tvName", ""));
        tvRoom.setText(aaa.getString("tvRoom", ""));
        cbAudioOnly.setChecked(aaa.getBoolean("cbAudioOnly", false));
        cbMulti.setChecked(aaa.getBoolean("cbMulti", false));
        btStart.setOnClickListener(v -> {
            saveInfo();
            MSManager.User user = new MSManager.User();
            user.setId(tvId.getText().toString());
            user.setDisplayName(tvName.getText().toString());
            user.setAvatar(icons[new Random().nextInt(icons.length)]);
            ProgressDialog dialog = ProgressDialog.show(this, "创建会话中...", null, true, false);
            MSManager.createRoom(tvRoom.getText().toString(), new MSManager.Callback<String>() {
                @Override
                public void onFail(Throwable e) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    dialog.dismiss();
                    MSManager.startCall(MainActivity.this, s, user,
                            cbAudioOnly.isChecked(), cbMulti.isChecked(), true,
                            userList);
                }
            });
        });
        btJoin.setOnClickListener(v -> {
            saveInfo();
            MSManager.User user = new MSManager.User();
            user.setId(tvId.getText().toString());
            user.setDisplayName(tvName.getText().toString());
            user.setAvatar(icons[new Random().nextInt(icons.length)]);
            ProgressDialog dialog = ProgressDialog.show(this, "连接会话中...", null, true, false);
            MSManager.roomExists(tvRoom.getText().toString(), new MSManager.Callback<Boolean>() {
                @Override
                public void onFail(Throwable e) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(Boolean s) {
                    dialog.dismiss();
                    if (s) {
                        MSManager.startCall(MainActivity.this, tvRoom.getText().toString(), user,
                                cbAudioOnly.isChecked(), cbMulti.isChecked(), false,
                                userList);
                    } else {
                        Toast.makeText(MainActivity.this, "会话已结束", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
        btBusy.setOnClickListener(v -> {
            ProgressDialog dialog = ProgressDialog.show(this, "请稍等...", null, true, false);
            MSManager.busy(tvRoom.getText().toString(), tvId.getText().toString(), new MSManager.Callback<Object>() {
                @Override
                public void onFail(Throwable e) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(Object aVoid) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    void saveInfo() {
        aaa.edit()
                .putString("tvId", tvId.getText().toString())
                .putString("tvName", tvName.getText().toString())
                .putString("tvRoom", tvRoom.getText().toString())
                .putBoolean("cbAudioOnly", cbAudioOnly.isChecked())
                .putBoolean("cbMulti", cbMulti.isChecked())
                .apply();
    }

    private void initView() {
        tvId = (AppCompatEditText) findViewById(R.id.tv_id);
        tvName = (AppCompatEditText) findViewById(R.id.tv_name);
        btStart = (AppCompatButton) findViewById(R.id.bt_start);
        cbAudioOnly = findViewById(R.id.cb_audioOnly);
        tvRoom = findViewById(R.id.tv_room);
        tvUser = (AppCompatTextView) findViewById(R.id.tv_user);
        btSelect = (AppCompatButton) findViewById(R.id.bt_select);
        cbMulti = (AppCompatCheckBox) findViewById(R.id.cb_multi);
        btJoin = (AppCompatButton) findViewById(R.id.bt_join);
        btBusy = (AppCompatButton) findViewById(R.id.bt_busy);
    }

    public void onPick(View view) {
        startActivityForResult(new Intent(this, SelectUserActivity.class), 0);
    }

    void setUserText() {
        StringBuilder stringBuilder = new StringBuilder();
        for (MSManager.User user : userList) {
            stringBuilder.append(user.getId())
                    .append("-")
                    .append(user.getDisplayName())
                    .append("   ");
        }
        tvUser.setText(stringBuilder.toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            List<SelectUserActivity.User> arrayList = (List<SelectUserActivity.User>) data.getSerializableExtra("data");
            userList.addAll(arrayList);
            setUserText();
        }
    }
}