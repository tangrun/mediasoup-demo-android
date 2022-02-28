package com.tangrun.mschat;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

/**
 * @author RainTang
 * @description:
 * @date :2021/11/5 14:12
 */
public class AudioPlayHelper {
    private static final String TAG = "AudioPlayHelper";
    private static MediaPlayer player;


    public static void stop() {
        Log.d(TAG, "stopRing() called");
        if (player != null) {
            try {
                player.stop();
            } catch (Exception e) {
                e.printStackTrace();
                player = null;
                player = new MediaPlayer();
                player.stop();
            }
            player.release();
            player = null;
        }
    }

    public static void startRingForType(Context context,int id, boolean loop) {
        startRingForType(context,id, loop, false);
    }

    public static void startRingForType(Context context,int id, boolean loop, boolean endClose) {
        Log.d(TAG, "startRingForType() called with: id = [" + id + "], loop = [" + loop + "]");
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + id);
        if (player == null) {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_RING);
        } else {
            try {
                player.stop();
            } catch (Exception e) {
                e.printStackTrace();
                player = null;
                player = new MediaPlayer();
                player.setAudioStreamType(AudioManager.STREAM_RING);
                player.stop();
            }
        }
        if (uri == null) return;
        try {
            player.reset();
            player.setDataSource(context, uri);
            player.setLooping(loop);
            player.prepare();
            player.start();
            if (endClose && !loop)
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stop();
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
