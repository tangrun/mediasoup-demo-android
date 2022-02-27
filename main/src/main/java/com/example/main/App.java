package com.example.main;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.tangrun.mschat.MSManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/17 14:04
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        MSManager.init(this, "ms.trhd11.xyz", null, BuildConfig.DEBUG);
        MSManager.init(this, "192.168.0.218", "4443", BuildConfig.DEBUG);
        MSManager.setUiCallback(new MSManager.UICallback() {
            @Override
            public Intent onAddUser(Context context, List<MSManager.User> users) {
                return new Intent(context, SelectUserActivity.class);
            }

            @Override
            public void onAddUserResult(int resultCode, Intent intent) {
                if (resultCode != Activity.RESULT_OK) return;
                List<SelectUserActivity.User> arrayList = (List<SelectUserActivity.User>) intent.getSerializableExtra("data");
                List<MSManager.User> list = new ArrayList<>(arrayList);
                MSManager.addUser(list);
            }
        });
    }
}
