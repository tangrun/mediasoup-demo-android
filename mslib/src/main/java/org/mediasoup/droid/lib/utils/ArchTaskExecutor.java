package org.mediasoup.droid.lib.utils;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ArchTaskExecutor {
    private static volatile ArchTaskExecutor sInstance;

    private final Object mLock = new Object();

    private final ScheduledExecutorService mDiskIO = Executors.newScheduledThreadPool(4, new ThreadFactory() {
        private static final String THREAD_NAME_STEM = "m_arch_disk_io_%d";

        private final AtomicInteger mThreadId = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(String.format(THREAD_NAME_STEM, mThreadId.getAndIncrement()));
            return t;
        }
    });

    @Nullable
    private volatile Handler mMainHandler;

    @NonNull
    private static final Executor sMainThreadExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            getInstance().postToMainThread(command);
        }
    };

    @NonNull
    private static final Executor sIOThreadExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            getInstance().executeOnDiskIO(command);
        }
    };

    @NonNull
    public static ArchTaskExecutor getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (androidx.arch.core.executor.ArchTaskExecutor.class) {
            if (sInstance == null) {
                sInstance = new ArchTaskExecutor();
            }
        }
        return sInstance;
    }

    public void executeOnDiskIO(Runnable runnable) {
        mDiskIO.execute(runnable);
    }

    public void executeOnDiskIO(Runnable runnable, long delayMillis) {
        mDiskIO.schedule(runnable, delayMillis, TimeUnit.MILLISECONDS);
    }


    public void postToMainThread(Runnable runnable) {
        if (mMainHandler == null) {
            synchronized (mLock) {
                if (mMainHandler == null) {
                    mMainHandler = createAsync(Looper.getMainLooper());
                }
            }
        }
        //noinspection ConstantConditions
        mMainHandler.post(runnable);
    }

    public void postToMainThread(Runnable runnable,long delayMillis) {
        if (mMainHandler == null) {
            synchronized (mLock) {
                if (mMainHandler == null) {
                    mMainHandler = createAsync(Looper.getMainLooper());
                }
            }
        }
        //noinspection ConstantConditions
        mMainHandler.postDelayed(runnable,delayMillis);
    }

    @NonNull
    public static Executor getMainThreadExecutor() {
        return sMainThreadExecutor;
    }

    @NonNull
    public static Executor getIOThreadExecutor() {
        return sIOThreadExecutor;
    }

    public boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private static Handler createAsync(@NonNull Looper looper) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Handler.createAsync(looper);
        }
        if (Build.VERSION.SDK_INT >= 16) {
            try {
                return Handler.class.getDeclaredConstructor(Looper.class, Handler.Callback.class,
                                boolean.class)
                        .newInstance(looper, null, true);
            } catch (IllegalAccessException ignored) {
            } catch (InstantiationException ignored) {
            } catch (NoSuchMethodException ignored) {
            } catch (InvocationTargetException e) {
                return new Handler(looper);
            }
        }
        return new Handler(looper);
    }

}
