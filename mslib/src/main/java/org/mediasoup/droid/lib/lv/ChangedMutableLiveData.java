package org.mediasoup.droid.lib.lv;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;
import androidx.lifecycle.MutableLiveData;

@SuppressWarnings("WeakerAccess")
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
