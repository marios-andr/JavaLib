package io.github.congueror.spotify.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.congueror.spotify.SpotifyObject;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class Track extends SpotifyObject {
    private static final Map<String, Track> CACHE = new ConcurrentHashMap<>();
    private final String name;
    private final String artist;

    private Track(String id, String name, String artist) {
        super(id);
        this.name = name;
        this.artist = artist;
        CACHE.put(id, this);
    }

    public static Track of(JsonObject json) {
        String id = json.get("id").getAsString();
        if (CACHE.containsKey(id))
            return CACHE.get(id);

        String name = json.get("name").getAsString();
        JsonArray artists = json.get("artists").getAsJsonArray();
        String artist = artists.get(0).getAsJsonObject().get("name").getAsString();
        return new Track(id, name, artist);
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Track track = (Track) o;
        return Objects.equals(name, track.name) && Objects.equals(artist, track.artist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, artist);
    }

    @Override
    public String toString() {
        return "Track{" +
                "name='" + name + '\'' +
                ", artist='" + artist + '\'' +
                '}';
    }
}
