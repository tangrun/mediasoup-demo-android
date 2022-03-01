package com.tangrun.mschat;

public interface ApiCallback<T> {
    void onFail(Throwable e);

    void onSuccess(T t);
}
