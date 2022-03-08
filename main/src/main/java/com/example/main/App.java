package com.example.main;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import android.util.Log;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.UICallback;
import com.tangrun.mschat.enums.CallEnd;
import com.tangrun.mschat.enums.RoomType;
import com.tangrun.mschat.model.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/17 14:04
 */
public class App extends Application {

    private static final String TAG = "MS_App";

    @Override
    public void onCreate() {
        super.onCreate();
        MSManager.init(this, "192.168.0.218", "4443", BuildConfig.DEBUG, new UICallback() {
//        MSManager.init(this, "ms.trhd11.xyz", null, BuildConfig.DEBUG, new UICallback() {
            @Override
            public Intent getAddUserIntent(Context context,String roomId, RoomType roomType, boolean audioOnly, List<User> users) {
                return new Intent(context, SelectUserActivity.class);
            }

            @Override
            public void onAddUserResult(int resultCode, Intent intent) {
                if (resultCode != Activity.RESULT_OK) return;
                List<SelectUserActivity.User> arrayList = (List<SelectUserActivity.User>) intent.getSerializableExtra("data");
                List<User> list = new ArrayList<>(arrayList);
                MSManager.addUser(list);
            }

            @Override
            public void onCallEnd(String id, RoomType roomType, boolean audioOnly, CallEnd callEnd, Date start, Date end) {
                Log.d(TAG, "onCallEnd() called with: id = [" + id + "], roomType = [" + roomType + "], audioOnly = [" + audioOnly + "], callEnd = [" + callEnd + "], start = [" + start + "], end = [" + end + "]");
            }
        });
    }
}
