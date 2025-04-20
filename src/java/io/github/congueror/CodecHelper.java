package io.github.congueror;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class CodecHelper {
    private static final SecureRandom RAND = new SecureRandom();

    public static String generateRandomString(int length) {
        String possibles = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int number = RAND.nextInt(possibles.length());
            sb.append(possibles.charAt(number));
        }
        return sb.toString();
    }

    public static String generateChallenger(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.US_ASCII));
            return base64URLEncode(digest);
        } catch (Exception e) {
            return null;
        }
    }

    public static String base64URLEncode(String input) {
        return base64URLEncode(input.getBytes(StandardCharsets.US_ASCII));
    }

    public static String base64URLEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }
}
