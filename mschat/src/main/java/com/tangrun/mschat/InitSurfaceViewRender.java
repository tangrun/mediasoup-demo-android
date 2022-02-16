package com.tangrun.mschat;

import android.content.Context;
import android.util.AttributeSet;

import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.webrtc.SurfaceViewRenderer;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/16 16:26
 */
public class InitSurfaceViewRender extends SurfaceViewRenderer {
    public InitSurfaceViewRender(Context context) {
        super(context);
    }

    public InitSurfaceViewRender(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    boolean init= false;
    public void init(){
        if (init)return;
        init = true;
        init(PeerConnectionUtils.getEglContext(),null);
    }
}
