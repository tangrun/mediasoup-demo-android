package com.tangrun.mslib.lv;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiMutableLiveData extends MutableLiveData<Void> {

    private Map<LiveData<?>,Observer<?>> map = new ConcurrentHashMap<>();

    public MultiMutableLiveData() {
    }

    public <S> void addSource(LiveData<S> liveData){
        if (map.containsKey(liveData)) return;
        Observer<S> observer = new Observer<S>() {
            @Override
            public void onChanged(S s) {
                setValue(null);
            }
        };
        map.put(liveData, observer);
        liveData.observeForever(observer);
    }
}
