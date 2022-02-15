package com.tangrun.mschat;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.example.mschat.R;

import org.mediasoup.droid.lib.lv.RoomStore;

public class RoomActivity extends AppCompatActivity {


    private FrameLayout container;
    RoomStore roomStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        RoomViewModel viewModel = ViewModelProviders.of(this).get(RoomViewModel.class);
        //RoomViewModel viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(RoomViewModel.class);
        viewModel.setRoomStore(Start.getClient());
        int type = getIntent().getIntExtra("type", 1);
        if (type == 0){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, SingleFragment.newInstance())
                    .commit();
        }else if (type == 1){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, MultiFragment.newInstance())
                    .commit();
        }
    }



}