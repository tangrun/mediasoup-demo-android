package com.tangrun.mschat.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.tangrun.mschat.model.UIRoomStore;

public class UserSelector extends AppCompatActivity {

    public static void start(Context context, Intent intent) {
        if (intent == null) return;
        context.startActivity(new Intent(context, UserSelector.class).putExtra("intent", intent));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent().getParcelableExtra("intent");
        if (intent == null) {
            finish();
            return;
        }
        // 修复立即会返回cancel的问题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (UIRoomStore.getCurrent() != null) {
                    UIRoomStore.getCurrent().onAddUserResult(result.getResultCode(), result.getData());
                }
                finish();
            }
        }).launch(intent);
    }
}