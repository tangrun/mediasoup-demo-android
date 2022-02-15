package com.example.main;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.tangrun.mschat.Start;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Start.startMultiCall(this, "1", "123", "Rain Tang", null);
    }
}