import com.google.gson.JsonObject;
import io.github.congueror.FileHelper;
import io.github.congueror.JsonHelper;
import io.github.congueror.NetworkHelper;
import io.github.congueror.spotify.SpotifyApi;
import io.github.congueror.spotify.objects.Track;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class SpotifyDownloader {
    private final Path outputPath;
    private final SpotifyApi sp;
    private final Path tracksPath;
    private final JsonObject tracksJson;

    public SpotifyDownloader(Path outputPath, Path settingsPath) {
        this.outputPath = outputPath;
        this.tracksPath = outputPath.resolve("tracks.json");

        JsonObject obj = JsonHelper.readFromFile(settingsPath);
        if (!obj.has("clientId")) {
            obj.addProperty("clientId", "HERE");
            JsonHelper.saveToFile(obj, settingsPath);
            throw new IllegalStateException("Missing field: clientId in " + settingsPath);
        }
        String clientId = obj.get("clientId").getAsString();
        sp = new SpotifyApi(clientId);
        sp.awaitReady();

        tracksJson = JsonHelper.readFromFile(tracksPath);
    }

    public CompletableFuture<Void> start(Predicate<List<Track>> shouldStart) {
        List<Track> tracks = sp.getUserSavedTracks();
        List<String> savedTracks = getSavedTracks();

        List<Track> notSavedTracks = new ArrayList<>();
        for (Track t : tracks) {
            if (!savedTracks.contains(t.getId()))
                notSavedTracks.add(t);
        }

        final List<Track> finalTracks = Collections.unmodifiableList(notSavedTracks);
        if (!shouldStart.test(finalTracks))
            return null;

        return CompletableFuture.runAsync(() -> this.startTrackDownload(finalTracks)).thenRun(() -> {
            JsonHelper.saveToFile(this.tracksJson, tracksPath);
        });
    }

    private List<String> getSavedTracks() {
        if (!tracksJson.has("tracks") || !tracksJson.get("tracks").isJsonObject()) {
            tracksJson.remove("tracks");
            tracksJson.add("tracks", new JsonObject());
            JsonHelper.saveToFile(tracksJson, tracksPath);
            return Collections.emptyList();
        }

        List<String> tracks = new ArrayList<>();
        JsonObject trsjhsdf = tracksJson.getAsJsonObject("tracks");
        for (String key : trsjhsdf.keySet()) {
            Path p = Path.of(trsjhsdf.get(key).getAsString());
            if (Files.exists(p))
                tracks.add(key);
        }
        return tracks;
    }

    private void startTrackDownload(List<Track> tracks) {
        Path exe;
        try {
            exe = FileHelper.extractResourceFile("yt-dlp.exe", outputPath);
        } catch (Exception e) {
            System.out.println("Something went wrong while extracting yt-dlp.exe .");
            e.printStackTrace();
            return;
        }

        File exeFile = exe.toFile();
        exeFile.deleteOnExit();

        try (FileWriter fw = FileHelper.writeTempFile(outputPath)) {
            for (Track track : tracks) {
                String trackName = track.getName() + " by " + track.getArtist() + " sound";
                fw.write("ytsearch1:" + trackName + "\n");
            }

        } catch (Exception e) {
            System.out.println("Something went wrong while writing to temp.txt .");
            e.printStackTrace();
        }

        try {
            new ProcessBuilder(exeFile.getPath(), "-U").start(); //Update yt-dlp
        } catch (IOException e) {
            System.out.println("Something went wrong while updating yt-dlp.exe .");
        }

        //String curl = encodedCurl(outputPath, "http://localhost:8080?filepath=\\\"%(filepath)s\\\"");
        String curl = encodedCurl(outputPath, "%(filepath)s");
        ProcessBuilder pb = new ProcessBuilder(exeFile.getPath(),
                "--batch-file", outputPath.resolve("temp.txt").toString(),
                "-P", outputPath.toString(),
                "-o", "%(title)s.%(ext)s",
                "-f", "140",
                "--exec", curl
        );

        try {
            startListener(tracks);
            Process process = pb.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveTrack(String filepath, Track track) {
        tracksJson.getAsJsonObject("tracks").addProperty(track.getId(), filepath);
    }

    private void startListener(List<Track> tracks) {
        NetworkHelper.listenCallbackAsync(8080, (in) -> {
            try {
                int counter = 0;
                do {
                    String cmd = in.readLine();
                    if (cmd == null) return; //client is hung up
                    cmd = cmd.trim();

                    if (cmd.startsWith("GET")) {
                        String path = NetworkHelper.getURLQueryParam(NetworkHelper.trimGetRequest(cmd), "filepath");
                        if (path != null) {
                            saveTrack(path, tracks.get(counter++));
                        }
                    }
                } while (counter != tracks.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String encodedCurl(Path outputFolder, String url) {
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("windows");
        Path script;
        try {
            if (isWin) {
                script = FileHelper.extractResourceFile("encoded-curl.ps1", outputFolder);
                outputFolder.resolve("encoded-curl.ps1").toFile().deleteOnExit();
                return "powershell.exe -executionpolicy bypass -file \\\"" + script.toAbsolutePath() + "\\\" \\\"" + url + "\\\"";
            } else {
                script = FileHelper.extractResourceFile("encoded-curl.sh", outputFolder);//TODO: Android/Unix compat
            }
        } catch (Exception e) {
            System.out.println("Something went wrong while extracting encoded-url script.");
            System.out.println(e);
            return null;
        }
        if (script == null)
            return null;
        return "&\\\"" + script.toAbsolutePath() + "\\\" \\\"" + url + "\\\"";
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Download Location (Relative to the current directory): ");
        Path output = null;
        while (output == null) {
            try {
                output = Paths.get(scanner.nextLine());
                //if (!output.toFile().isDirectory()) {
                //    output = null;
                //    throw new RuntimeException();
                //}
            } catch (RuntimeException e) {
                System.out.println("The path is invalid. Try again.");
            }
        }

        Path settingsPath = Path.of("./settings.json");
        SpotifyDownloader sd = new SpotifyDownloader(output, settingsPath);
        CompletableFuture<Void> download = sd.start(tracks -> {
            System.out.println("Found " + tracks.size() + " tracks that are not present in the given directory: ");
            for (Track track : tracks) {
                System.out.println(track.getName());
            }
            System.out.println("Would you like to download these tracks? (y/n)");
            String answer = scanner.next().toLowerCase();
            if (answer.equals("y")) {
                return true;
            }
            return false;
        });
        download.join();
    }
}
