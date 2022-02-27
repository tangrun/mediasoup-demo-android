package org.mediasoup.droid.lib.lv;

import android.database.Observable;
import org.mediasoup.droid.lib.model.Buddy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

public class BuddyObservable extends Observable<BuddyObserver>   {
    private final BuddyObserver dispatcher = (BuddyObserver) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{BuddyObserver.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class)
                return method.invoke(this, args);
            for (BuddyObserver mObserver : mObservers) {
                method.invoke(mObserver, args);
            }
            return null;
        }
    });

    public BuddyObserver dispatcher(){
        return dispatcher;
    }

}
