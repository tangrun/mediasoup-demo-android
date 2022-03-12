package com.tangrun.mschat.ui;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import com.tangrun.mschat.MSManager;
import org.mediasoup.droid.Logger;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "MS_CallReceiver";
    TelephonyManager telephonyManager;
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d(TAG, "onReceive: "+intent.getAction());
        if ("android.intent.action.PHONE_STATE".equals(intent.getAction())) {
            if (telephonyManager == null){
                telephonyManager =      (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
            }
            switch (telephonyManager.getCallState()) {
                case TelephonyManager.CALL_STATE_RINGING:
                    // 响铃
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // 开始通话
                    if (MSManager.getCurrent() != null){
                        MSManager.getCurrent().hangup();
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // 通话结束
                    break;
            }

        }
    }


}
