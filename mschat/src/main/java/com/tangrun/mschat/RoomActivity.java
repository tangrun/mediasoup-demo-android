package com.tangrun.mschat;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mschat.R;

public class RoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        UIRoomStore uiRoomStore = MSManager.getCurrent();
        if (uiRoomStore==null) {
            finish();
            return;
        }
        if (uiRoomStore.roomType == 0){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, SingleFragment.newInstance())
                    .commit();
        }else if (uiRoomStore.roomType == 1){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, MultiFragmentV1.newInstance())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {

    }

    public void onBack(){
        super.onBackPressed();
    }

}