package org.mediasoup.droid.lib.lv;

import android.database.Observable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ClientObservable extends Observable<ClientObserver>   {
    private final ClientObserver dispatcher = (ClientObserver) Proxy.newProxyInstance(ClientObservable.class.getClassLoader(), new Class[]{ClientObserver.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class)
                return method.invoke(this, args);
            for (ClientObserver mObserver : mObservers) {
                method.invoke(mObserver, args);
            }
            return null;
        }
    });

    public ClientObserver dispatcher(){
        return dispatcher;
    }

}
