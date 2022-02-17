package com.tangrun.mschat;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mschat.R;

import org.mediasoup.droid.lib.lv.RoomStore;

public class RoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        UIRoomStore uiRoomStore = Start.getCurrent();
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
                    .add(R.id.container, MultiFragment.newInstance())
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