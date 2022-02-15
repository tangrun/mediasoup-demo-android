package org.mediasoup.droid.lib;

import org.json.JSONArray;
import org.mediasoup.droid.Consumer;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/15 8:50
 */
public class WrapperCommon {
    protected boolean mLocallyPaused;
    protected boolean mRemotelyPaused;
    protected JSONArray mScore;

    public boolean isLocallyPaused() {
        return mLocallyPaused;
    }

    public boolean isRemotelyPaused() {
        return mRemotelyPaused;
    }

    public JSONArray getScore() {
        return mScore;
    }
}
