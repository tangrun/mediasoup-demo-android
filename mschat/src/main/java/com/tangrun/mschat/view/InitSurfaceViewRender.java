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
import org.webrtc.*;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/16 16:26
 */
public class InitSurfaceViewRender extends SurfaceViewRenderer {
    private static final String TAG = "MS_ViewRender";

    public InitSurfaceViewRender(Context context) {
        super(context);
    }

    public InitSurfaceViewRender(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    boolean init = false;

    @Override
    public void release() {
        if (!init) return;
        super.release();
        init = false;
    }

    @Override
    public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents) {
        if (init) return;
        super.init(sharedContext, rendererEvents);
        init = true;
    }

    @Override
    public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents, int[] configAttributes, RendererCommon.GlDrawer drawer) {
        if (init) return;
        super.init(sharedContext, rendererEvents, configAttributes, drawer);
        init = true;
    }
}
