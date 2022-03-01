package com.tangrun.mschat;

import android.content.Context;
import android.content.Intent;
import com.tangrun.mschat.enums.CallEnd;
import com.tangrun.mschat.enums.RoomType;
import com.tangrun.mschat.model.User;

import java.util.Date;
import java.util.List;

public interface UICallback {
    Intent getAddUserIntent(Context context, List<User> users);

    void onAddUserResult(int resultCode, Intent intent);

    void onCallEnd(String id, RoomType roomType, boolean audioOnly, CallEnd callEnd, Date start, Date end);
}
