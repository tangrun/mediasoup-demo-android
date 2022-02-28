package com.tangrun.mschat.ui;

import android.os.Bundle;

import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mschat.R;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.enums.RoomType;
import com.tangrun.mschat.model.UIRoomStore;

public class CallRoomActivity extends AppCompatActivity {
    private static final String TAG = "MS_CallRoomActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ms_call_room);
        UIRoomStore uiRoomStore = MSManager.getCurrent();
        if (uiRoomStore == null) {
            finish();
            return;
        }
        uiRoomStore.bindLifeOwner(this);
        uiRoomStore.finished.observe(this,aBoolean -> {
            if (aBoolean == Boolean.TRUE)
                finish();
        });
        uiRoomStore.showActivity.applySet(true);
        uiRoomStore.showActivity.observe(this,aBoolean -> {
            if (aBoolean == Boolean.FALSE)
                onBack();
        });

        switch (uiRoomStore.roomType) {
            case SingleCall: {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, SingleCallRoomFragment.newInstance())
                        .commit();
                break;
            }
            case MultiCall: {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, MultiCallRoomFragment.newInstance())
                        .commit();
                break;
            }
            default: {
                finish();
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {

    }

    public void onBack() {
        Log.d(TAG, "onBack: ");
        super.onBackPressed();
    }

}