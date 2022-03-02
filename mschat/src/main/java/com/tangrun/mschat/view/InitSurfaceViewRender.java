package com.tangrun.mschat.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import com.tangrun.mslib.utils.PeerConnectionUtils;
import org.jetbrains.annotations.NotNull;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/16 16:26
 */
public class InitSurfaceViewRender extends SurfaceViewRenderer implements LifecycleEventObserver {
    private static final String TAG = "MS_ViewRender";
    public InitSurfaceViewRender(Context context) {
        super(context);
    }

    public InitSurfaceViewRender(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    boolean init = false;
    boolean register = false;
    VideoTrack videoTrack;

    public void init(LifecycleOwner lifecycleOwner) {
        if (register) return;
        Log.d(TAG, hashCode()+"init");
        register = true;
        lifecycleOwner.getLifecycle().addObserver(this);
    }

    public void bind(LifecycleOwner lifecycleOwner,boolean removeOldTrackSink, VideoTrack videoTrack) {
        Log.d(TAG, hashCode()+"bind: 1");
        if (removeOldTrackSink && this.videoTrack != null ) this.videoTrack.removeSink(this);
        Log.d(TAG, hashCode()+"bind: 2");
        this.videoTrack = videoTrack;
        if (lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            Log.d(TAG, hashCode()+"bind: STARTED ");
            bind();
        }
    }

    private void bind() {
        if (!register) return;
        if (videoTrack != null) {
            if (!init) {
                Log.d(TAG, hashCode()+"bind: inner 1");
                init(PeerConnectionUtils.getEglContext(), null);
                init = true;
                Log.d(TAG, hashCode()+"bind: inner 2");
            }
            videoTrack.addSink(this);
            Log.d(TAG, hashCode()+"bind: inner 3");
        } else unbind();
    }

    private void unbind() {
        if (!register) return;
        if (init) {
            init = false;
            Log.d(TAG, hashCode()+"unbind: release 1");
            release();
            Log.d(TAG, hashCode()+"unbind: release 2");
        }
        if (videoTrack != null) {
            Log.d(TAG, hashCode()+"unbind: release 3");
            videoTrack.removeSink(this);
            Log.d(TAG, hashCode()+"unbind: release 4");
        }
    }

    @Override
    public void onStateChanged(@NonNull @NotNull LifecycleOwner source, @NonNull @NotNull Lifecycle.Event event) {
        Log.d(TAG, hashCode()+"onStateChanged: "+event +" videoTrack = "+videoTrack+" init = "+init);
        if (event == Lifecycle.Event.ON_DESTROY) {
            source.getLifecycle().removeObserver(this);
            unbind();
            videoTrack = null;
        } else if (event == Lifecycle.Event.ON_RESUME) {
            bind();
        } else if (event == Lifecycle.Event.ON_STOP) {
            unbind();
        }
    }
}
