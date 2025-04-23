import io.github.congueror.FileHelper;
import io.github.congueror.JsonHelper;
import io.github.congueror.NetworkHelper;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        JsonHelper.readFromFile(Path.of("a/").resolve("b.json"));

        //System.out.println("Listening...");
        //String aa = NetworkHelper.listenCallback(8080, s -> s.startsWith("GET")).join();
        //System.out.println(aa);
        //System.out.println(NetworkHelper.getURLQueryParam(NetworkHelper.trimGetRequest(aa), "filepath"));

    }
}