package org.mediasoup.droid.lib.lv;

import android.database.Observable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ObservableMap<K, V> extends Observable<ObservableMap.OnArrayListListener<ObservableMap<K, V>, K, V>> implements Map<K, V> {
    private final Map<K, V> map = new ConcurrentHashMap<>();


    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(@Nullable @org.jetbrains.annotations.Nullable Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable @org.jetbrains.annotations.Nullable Object value) {
        return map.containsValue(value);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public V get(@Nullable @org.jetbrains.annotations.Nullable Object key) {
        return map.get(key);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public V put(@NonNull @NotNull K key, @NonNull @NotNull V value) {
        V v = map.put(key, value);
        if (v == null) {
            for (OnArrayListListener<ObservableMap<K, V>, K, V> listener : mObservers) {
                listener.onPut(this, key, value);
            }
        } else {
            for (OnArrayListListener<ObservableMap<K, V>, K, V> listener : mObservers) {
                listener.onUpdate(this, key, value);
            }
        }
        return v;
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public V remove(@Nullable @org.jetbrains.annotations.Nullable Object key) {
        V v = map.remove(key);
        if (v != null) {
            for (OnArrayListListener<ObservableMap<K, V>, K, V> listener : mObservers) {
                listener.onRemove(this, (K) key);
            }
        }
        return v;
    }

    @Override
    public void putAll(@NonNull @NotNull Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @NonNull
    @NotNull
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @NonNull
    @NotNull
    @Override
    public Collection<V> values() {
        return map.values();
    }

    @NonNull
    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    public interface OnArrayListListener<T, K, V> {
        void onPut(T sender, K key, V value);

        void onUpdate(T sender, K key, V value);

        void onRemove(T sender, K key);

    }
}
