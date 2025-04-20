import com.google.gson.JsonObject;
import io.github.congueror.FileHelper;
import io.github.congueror.JsonHelper;
import io.github.congueror.NetworkHelper;
import io.github.congueror.spotify.SpotifyApi;
import io.github.congueror.spotify.objects.Track;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

public class SpotifyDownloader {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Download Location (Relative to the current directory): ");
        Path output = null;
        while (output == null) {
            try {
                output = Paths.get(scanner.nextLine());
            } catch (InvalidPathException e) {
                System.out.println("The path is invalid. Try again.");
            }
        }

        JsonObject obj = JsonHelper.readFromFile(Path.of("./settings.json"));
        if (!obj.has("clientId")) {
            System.out.println("No client ID was found in settings.json. The program will exit.");
            return;
        }
        String clientId = obj.get("clientId").getAsString();
        SpotifyApi sp = new SpotifyApi(clientId);
        sp.awaitReady();

        List<Track> tracks = sp.getUserSavedTracks();

        System.out.println("Found " + tracks.size() + " tracks that are not present in the given directory: ");
        for (Track track : tracks) {
            System.out.println(track.getName());
        }
        System.out.println("Would you like to download these tracks? (y/n)");
        String answer = scanner.next().toLowerCase();
        if (answer.equals("y")) {
            startTrackDownload(output, tracks);
        }
    }

    private static void startTrackDownload(Path outputPath, List<Track> tracks) {
        Path exe;
        try {
            exe = extractExe(outputPath);
        } catch (Exception e) {
            System.out.println("Something went wrong while extracting yt-dlp.exe .");
            e.printStackTrace();
            return;
        }

        File outDir = outputPath.toFile();
        outDir.mkdirs();

        File exeFile = exe.toFile();

        try (FileWriter fw = FileHelper.writeTempFile(outputPath)) {
            for (Track track : tracks) {
                String trackName = track.getName() + " by " + track.getArtist() + " sound";
                fw.write("ytsearch1:"+trackName+"\n");
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

        ProcessBuilder pb = new ProcessBuilder(exeFile.getPath(),
                "--batch-file", outputPath.resolve("temp.txt").toString(),
                "-P", outputPath.toString(),
                "-o", "%(title)s.%(ext)s",
                "-f", "140",
                "--exec", "curl http://localhost:8080?filepath=%(filepath)s"
        );

        try {
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

    private static Path extractExe(Path outputFolder) throws URISyntaxException, IOException {
        URI jar = SpotifyDownloader.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        String fileName = "yt-dlp.exe";
        File loc = new File(jar);
        Path filePath;
        if (loc.isDirectory())
            filePath = Paths.get(jar).resolve(fileName);
        else {
            try (ZipFile zipFile = new ZipFile(loc)) {
                filePath = FileHelper.extractFile(zipFile, fileName, outputFolder);
                filePath.toFile().deleteOnExit();
            }
        }

        return filePath;
    }

    private static void applyMetadata(String filepath, Track track) {
        //Ignore for now
    }

    private static void startListener(List<Track> tracks) {
        AtomicInteger counter = new AtomicInteger();
        var future = nextListener(counter, tracks);
    }

    private static CompletableFuture<String> nextListener(AtomicInteger counter, List<Track> tracks) {
        if (counter.get() >= tracks.size()) {
            System.out.println("All tracks processed. Stopping listener.");
            return CompletableFuture.completedFuture(null); // Exit condition
        }

        return NetworkHelper.listenCallback(8080, s -> s.startsWith("GET"))
                .thenCompose(s -> {
                    var next = nextListener(counter, tracks);
                    String filepath = NetworkHelper.getURLQueryParam(NetworkHelper.trimGetRequest(s), "filepath");
                    applyMetadata(filepath, tracks.get(counter.getAndIncrement()));
                    return next;
                });
    }
}
