package com.tangrun.mschat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;

public class AddUserHandler extends AppCompatActivity {

    public static void start(Context context, List<MSManager.User> users) {
        if (MSManager.uiCallback == null) return;
        Intent intent = MSManager.uiCallback.onAddUser(context,users);
        if (intent == null) return;
        context.startActivity(new Intent(context, AddUserHandler.class).putExtra("intent", intent));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent().getParcelableExtra("intent");
        if (intent == null) {
            finish();
            return;
        }
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (MSManager.uiCallback != null) {
                MSManager.uiCallback.onAddUserResult(resultCode, data);
            }
        }
    }
}