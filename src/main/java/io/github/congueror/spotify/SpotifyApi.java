package io.github.congueror.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.congueror.CodecHelper;
import io.github.congueror.HttpRequestBuilder;
import io.github.congueror.JsonHelper;
import io.github.congueror.NetworkHelper;
import io.github.congueror.spotify.objects.Track;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpotifyApi {
    private static final String SPOTIFY_API = "https://api.spotify.com/v1/";
    private static final String CALLBACK_URI = "http://127.0.0.1:7070/callback";
    private static final Path DATA_PATH = Paths.get("./data.json");
    private final String clientId;
    private String codeVerifier;
    private String access_token;
    private String refresh_token;
    private String scope;
    private long last_refresh;

    private CompletableFuture<Void> task;

    public SpotifyApi(String clientId) {
        this.clientId = clientId;
        this.task = initialize();
    }

    public void awaitReady() {
        if (this.task == null)
            return;

        this.task.join();
        this.task = null;
    }

    private CompletableFuture<Void> initialize() {
        JsonObject data = JsonHelper.readFromFile(DATA_PATH);

        if (!data.has("tokens")) {
            return runAuthFlow();
        }

        data = data.get("tokens").getAsJsonObject();

        if (!JsonHelper.hasAll(data, "access_token", "refresh_token", "last_refresh", "scope")) {
            return runAuthFlow();
        }

        this.access_token = data.get("access_token").getAsString();
        this.refresh_token = data.get("refresh_token").getAsString();
        this.last_refresh = data.get("last_refresh").getAsLong();
        this.scope = data.get("scope").getAsString();

        if (System.currentTimeMillis() - this.last_refresh > 3600 * 1000) {
            return this.requestRefreshToken();
        }

        return null;
    }

    private CompletableFuture<JsonObject> sendApiRequest(HttpRequest request) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(JsonHelper::httpResponseToJson)
                    .thenApply(o -> {
                        if (o.get("status").getAsInt() == 200) {
                            return o;
                        } else if (o.get("status").getAsInt() == 400) {
                            System.out.println("Error: " + o.get("error").getAsJsonObject());
                            return null;
                        } else if (o.get("status").getAsInt() == 401) {
                            return requestRefreshToken().thenCompose(r -> sendApiRequest(request)).join();
                        } else if (o.get("status").getAsInt() == 403) {
                            System.out.println("Error: " + o.get("error").getAsJsonObject());
                            return null;
                        } else if (o.get("status").getAsInt() == 429) {
                            System.out.println("Error: " + o.get("error").getAsJsonObject());
                            return null;
                        }
                        System.out.println("Error: " + o.get("error").getAsJsonObject());
                        return null;
                    });
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong while sending request: " + request, e);
        }
    }

    private CompletableFuture<Void> runAuthFlow() {
        String code = this.authorizeUser();
        return this.requestAccessToken(code);
    }

    private String authorizeUser() {
        this.codeVerifier = CodecHelper.generateRandomString(64);
        String challenger = CodecHelper.generateChallenger(codeVerifier);

        StringBuilder uri = new StringBuilder("https://accounts.spotify.com/authorize?");
        uri.append("client_id=").append(clientId).append("&")
                .append("response_type=").append("code").append("&")
                .append("redirect_uri=").append(CALLBACK_URI).append("&")
                .append("scope=").append("user-library-read").append("&")
                .append("code_challenge_method=").append("S256").append("&")
                .append("code_challenge=").append(challenger);

        //System.out.println(uri);
        NetworkHelper.browse(uri.toString());

        CompletableFuture<String> ft = NetworkHelper.listenCallback(7070, s -> s.startsWith("GET"));
        try {
            String response = ft.join();
            String params = NetworkHelper.trimGetRequest(response);
            String code = NetworkHelper.getURLQueryParam(params, "code");
            if (code.isEmpty()) {
                String error = NetworkHelper.getURLQueryParam(params, "error");
                System.out.println("Callback returned error message: " + error);
                return null;
            }
            return code;
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong while getting/parsing the code callback!", e);
        }
    }

    private CompletableFuture<Void> requestAccessToken(String authCode) {
        URI url = URI.create("https://accounts.spotify.com/api/token");
        HttpRequest rb = new HttpRequestBuilder(url)
                .POST()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .addParameter("grant_type", "authorization_code")
                .addParameter("code", authCode)
                .addParameter("redirect_uri", CALLBACK_URI)
                .addParameter("client_id", clientId)
                .addParameter("code_verifier", codeVerifier)
                .build();

        CompletableFuture<Void> request = sendApiRequest(rb).thenAccept(this::parseAccessTokenResponse);
        awaitReady();
        return request;
    }

    private CompletableFuture<Void> requestRefreshToken() {
        URI url = URI.create("https://accounts.spotify.com/api/token");
        HttpRequest rb = new HttpRequestBuilder(url)
                .POST()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .addParameter("grant_type", "refresh_token")
                .addParameter("refresh_token", this.refresh_token)
                .addParameter("client_id", this.clientId)
                .build();

        CompletableFuture<Void> request = sendApiRequest(rb).thenAccept(this::parseAccessTokenResponse);
        awaitReady();
        return request;
    }

    private void parseAccessTokenResponse(JsonObject response) {
        if (response == null) {
            System.out.println("Request for access token/refresh token returned status code: " + response.get("status").getAsInt());
            return;
        }

        this.access_token = response.get("access_token").getAsString();
        this.refresh_token = response.get("refresh_token").getAsString();
        this.last_refresh = System.currentTimeMillis();
        this.scope = response.get("scope").getAsString();
        JsonObject json = JsonHelper.readFromFile(DATA_PATH);
        JsonObject accessToken = new JsonObject();
        accessToken.addProperty("access_token", this.access_token);
        accessToken.addProperty("refresh_token", this.refresh_token);
        accessToken.addProperty("last_refresh", this.last_refresh);
        accessToken.addProperty("scope", this.scope);
        json.add("tokens", accessToken);

        JsonHelper.saveToFile(json, DATA_PATH);
    }

    private CompletableFuture<JsonObject> getRequest(String endpoint) {
        URI url = URI.create(SPOTIFY_API + endpoint);
        HttpRequest rb = HttpRequest.newBuilder(url)
                .GET()
                .header("Authorization", "Bearer " + this.access_token)
                .build();
        CompletableFuture<JsonObject> request = sendApiRequest(rb);
        awaitReady();
        return request;
    }

    public List<Track> getUserSavedTracks() {
        List<Track> savedTracks = new ArrayList<>();
        String next = "me/tracks?limit=50&offset=0";
        do {
            var a = getRequest(next);
            JsonObject obj = a.join();
            if (obj.get("status").getAsInt() != 200) {
                break;
            }

            if (!obj.get("next").isJsonNull()) {
                next = obj.get("next").getAsString();
                next = next.substring(SPOTIFY_API.length());
            } else
                next = null;

            JsonArray tracks = obj.get("items").getAsJsonArray();
            for (JsonElement track : tracks) {
                JsonObject trackObj = track.getAsJsonObject().getAsJsonObject("track");
                savedTracks.add(Track.of(trackObj));
            }

        } while (next != null);

        return savedTracks;
    }

    public List<Track> getPlaylistTracks(String playlistId) {
        List<Track> savedTracks = new ArrayList<>();
        String next = "playlists/" + playlistId + "/tracks?limit=50&offset=0";

        do {
            var a = getRequest(next);
            JsonObject obj = a.join();
            if (obj.get("status").getAsInt() != 200) {
                break;
            }

            if (!obj.get("next").isJsonNull()) {
                next = obj.get("next").getAsString();
                next = next.substring(SPOTIFY_API.length());
            } else
                next = null;

            JsonArray tracks = obj.get("items").getAsJsonArray();
            for (JsonElement track : tracks) {
                JsonObject trackObj = track.getAsJsonObject().getAsJsonObject("track");
                savedTracks.add(Track.of(trackObj));
            }

        } while (next != null);

        return savedTracks;
    }
}
