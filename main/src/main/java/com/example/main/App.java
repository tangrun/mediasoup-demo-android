package com.example.main;

import android.app.Application;

import com.tangrun.mschat.Start;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/17 14:04
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //Start.init(this, "192.168.0.218", "4443", BuildConfig.DEBUG);
        Start.init(this, "ms.trhd11.xyz", null, BuildConfig.DEBUG);
    }
}
