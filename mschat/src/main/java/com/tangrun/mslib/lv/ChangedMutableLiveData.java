package com.tangrun.mslib.lv;

import androidx.lifecycle.MutableLiveData;

public class ChangedMutableLiveData<T> extends MutableLiveData<T> {

    public ChangedMutableLiveData(T value) {
        super(value);
    }

    public ChangedMutableLiveData() {
    }

    public void applyPost(T value) {
        if (getValue() != value) {
            postValue(value);
        }
    }

    public void applySet(T value) {
        if (getValue() != value) {
            setValue(value);
        }
    }
}
