import io.github.congueror.NetworkHelper;

public class Main {
    public static void main(String[] args) {
        String aa = NetworkHelper.listenCallback(8080, s -> s.startsWith("GET")).join();
        System.out.println(aa);
        System.out.println(NetworkHelper.getURLQueryParam(NetworkHelper.trimGetRequest(aa), "name"));
    }
}