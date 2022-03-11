package com.tangrun.mschat.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.R;
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
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON //保持屏幕长亮
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
        }

        uiRoomStore.bindActivity(this);
        uiRoomStore.showActivity.observe(this, aBoolean -> {
            if (aBoolean == Boolean.FALSE)
                finish();
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
        super.onBackPressed();
    }

}