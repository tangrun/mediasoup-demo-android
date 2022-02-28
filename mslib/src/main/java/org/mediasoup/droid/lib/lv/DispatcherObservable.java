package org.mediasoup.droid.lib.lv;

import android.database.Observable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DispatcherObservable<T> extends Observable<T> {

    private final T dispatcher ;

    public DispatcherObservable(Class<T> tClass) {
        dispatcher=  (T) Proxy.newProxyInstance(DispatcherObservable.class.getClassLoader(), new Class[]{tClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class)
                    return method.invoke(this, args);
                for (T mObserver : mObservers) {
                    method.invoke(mObserver, args);
                }
                return null;
            }
        });
    }

    public T getDispatcher() {
        return dispatcher;
    }
}
