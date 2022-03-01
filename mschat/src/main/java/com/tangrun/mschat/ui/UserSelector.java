package com.tangrun.mschat.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.model.UIRoomStore;
import com.tangrun.mschat.model.User;

import java.util.List;

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
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (UIRoomStore.getCurrent() != null) {
                UIRoomStore.getCurrent().onAddUserResult(resultCode, data);
            }
            finish();
        }
    }
}