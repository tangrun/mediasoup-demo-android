package com.tangrun.mschat.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.tangrun.mschat.MSManager;

import java.util.List;

public class UserSelector extends AppCompatActivity {

    public static void start(Context context, List<MSManager.User> users) {
        if (MSManager.getUiCallback() == null) return;
        Intent intent = MSManager.getUiCallback().onAddUser(context,users);
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
            if (MSManager.getUiCallback() != null) {
                MSManager.getUiCallback().onAddUserResult(resultCode, data);
            }
            finish();
        }
    }
}