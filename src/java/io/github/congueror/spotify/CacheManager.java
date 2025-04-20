package io.github.congueror.spotify;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager<I extends SpotifyObject> {
    private final Map<String, I> cache = new ConcurrentHashMap<>();
    private final long duration;

    public CacheManager(long duration) {
        this.duration = duration;
    }

    public void put(String key, I value) {
        cache.put(key, value);
    }

    public I get(String key) {
        I k = cache.get(key);
        if (isExpired(k))
            return null;
        return k;
    }

    public boolean has(String key) {
        return cache.containsKey(key) && cache.get(key) != null;
    }

    public boolean isExpired(I obj) {
        if (duration == -1)
            return false;
        return System.currentTimeMillis() - obj.getTimestamp() > duration;
    }
}
