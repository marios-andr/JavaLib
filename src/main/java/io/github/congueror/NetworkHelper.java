package io.github.congueror;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class NetworkHelper {


    public static CompletableFuture<String> listenCallbackAsync(int port) {
        return null;
    }

    public static CompletableFuture<String> listenCallback(int port, Predicate<String> filter) {
        try (ServerSocket ss = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))) {
            Socket s = ss.accept();//establishes connection
            var rawIn = s.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.US_ASCII));

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> parseCallbackReader(in, filter));

            return future;
        } catch (Exception e) {
            System.out.println(e);
            return CompletableFuture.completedFuture(null);
        }
    }

    //TODO: MAKE A FULL GET/POST PARSER
    //This currently only works for GET requests since all the data is within one line.
    private static String parseCallbackReader(BufferedReader in, Predicate<String> predicate) {
        String callback = "";

        try {
            while (true) {
                String cmd = in.readLine();
                if (cmd == null) break; //client is hung up
                cmd = cmd.trim();

                if (predicate.test(cmd)) {
                    callback = cmd;
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return callback;
    }

    public static void browse(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();

            if (os.contains("win")) {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                rt.exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                rt.exec("xdg-open " + url);
            }
        } catch (Exception e) {
            System.out.println("Something went wrong while opening browser url: " + url);
            e.printStackTrace();
        }
    }

    public static String trimGetRequest(String url) {
        String head = "GET ";
        String tail = " HTTP/1.1";
        return url.substring(head.length(), url.length() - tail.length()).trim();
    }

    public static String[] getURLQueryParams(String url) {
        try {
            URI u = new URI(url);
            String query = u.getQuery();
            String[] pairs = query.split("&");
            return pairs;
        } catch (Exception e) {
            System.out.println("Something went wrong while getting url query params: " + url);
            e.printStackTrace();
            return null;
        }
    }

    public static String getURLQueryParam(String url, String query) {
        String[] pairs = getURLQueryParams(url);
        if (pairs == null) return null;
        for (String pair : pairs) {
            if (pair.startsWith(query)) {
                return pair.substring(query.length()+1);
            }
        }
        return "";
    }
}
