package com.tangrun.mschat.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.*;
import com.tangrun.mschat.model.UIRoomStore;
import com.tangrun.mslib.enums.ConversationState;
import com.tangrun.mslib.enums.LocalConnectState;
import org.jetbrains.annotations.NotNull;

public class CallNotificationService extends LifecycleService {
    private static final String TAG = "MS_UI_NotifyService";

    private String notificationChannelId = null;
    private final int notificationId = 1;
    private final String notificationTag = "UIRoomStore";
    private static final String NOTIFICATION_CHANNEL_ID = "MSCall";
    private static final String NOTIFICATION_CHANNEL_NAME = "音视频通话";

    private NotificationManagerCompat notificationManagerCompat;

    UIRoomStore uiRoomStore;

    LifecycleEventObserver appProcessObserver = new LifecycleEventObserver() {
        @Override
        public void onStateChanged(@NonNull @NotNull LifecycleOwner source, @NonNull @NotNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_RESUME) {
                // 前台时刷新一下通知 防止进入时清空消息通知把通话通知也清掉了
                updateNotification();
            }
        }
    };

    @Override
    public int onStartCommand(@NonNull @NotNull Intent intent, int flags, int startId) {
        notificationManagerCompat = NotificationManagerCompat.from(this);
        // 通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setLightColor(0);
            channel.setSound(null, null);
            channel.setVibrationPattern(new long[]{});
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannelId = NOTIFICATION_CHANNEL_ID;
            NotificationManagerCompat.from(this).createNotificationChannel(channel);
        }
        startForeground(notificationId, getNotification());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        uiRoomStore = UIRoomStore.getCurrent();
        if (uiRoomStore == null) {
            stopSelf();
        }
    }

    @Override
    public void onStart(@NonNull @NotNull Intent intent, int startId) {
        super.onStart(intent, startId);
        uiRoomStore.localState.observeForever(localConnectStateConversationStatePair -> {
            updateNotification();
        });
        uiRoomStore.calling.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean == Boolean.FALSE) stopSelf();
            }
        });
        ProcessLifecycleOwner.get().getLifecycle().addObserver(appProcessObserver);
    }

    private void updateNotification() {
        notificationManagerCompat
                .notify(notificationId, getNotification());
    }

    private Notification getNotification() {
        ConversationState conversationState = uiRoomStore.localConversationState.getValue();
        LocalConnectState connectState = uiRoomStore.localConnectionState.getValue();

        String content = "";
        if (conversationState == ConversationState.New) {
            if (uiRoomStore.owner) {
                if (connectState == LocalConnectState.JOINED)
                    content = "等待对方接听";
                else {
                    content = "连接中...";
                }
            } else {
                content = "等待对方接听";
            }
        } else if (conversationState == ConversationState.Invited) {
            content = "待接听";
        } else if (conversationState == ConversationState.Joined) {
            content = "通话中...";
        } else {
            content = "通话已结束";
        }

        return new NotificationCompat.Builder(this, notificationChannelId)
                .setOngoing(true)
                .setSmallIcon(this.getApplicationInfo().icon)
                .setContentIntent(PendingIntent.getActivity(this, 0, uiRoomStore.getCallActivityIntent(), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentText(content)
                .build();
    }


}
