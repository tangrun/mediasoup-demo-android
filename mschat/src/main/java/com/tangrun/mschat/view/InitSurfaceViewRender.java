package com.tangrun.mschat.view;

import android.content.Context;
import android.util.AttributeSet;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

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
