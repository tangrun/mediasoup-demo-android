package com.example.main;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;

import com.tangrun.mschat.Start;

public class MainActivity extends AppCompatActivity {

    private AppCompatEditText tvId;
    private AppCompatEditText tvName;
    private AppCompatButton btStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        SharedPreferences aaa = getSharedPreferences("aaa", 0);
        tvId.setText(aaa.getString("id", ""));
        tvName.setText(aaa.getString("name", ""));
        btStart.setOnClickListener(v -> {
            aaa.edit()
                    .putString("id", tvId.getText().toString())
                    .putString("name", tvName.getText().toString())
                    .apply();
            Start.startMultiCall(this, "123", tvId.getText().toString(), tvName.getText().toString(), "http://data.17jita.com/attachment/block/22/22b2d5333288c835443b5d80e9995f16.jpg");
        });
    }

    private void initView() {
        tvId = (AppCompatEditText) findViewById(R.id.tv_id);
        tvName = (AppCompatEditText) findViewById(R.id.tv_name);
        btStart = (AppCompatButton) findViewById(R.id.bt_start);
    }
}