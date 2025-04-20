package io.github.congueror.spotify;

import java.util.Objects;

public abstract class SpotifyObject {
    protected final String id;
    private final long timestamp;

    public SpotifyObject(String id) {
        this.id = id;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SpotifyObject that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
