package com.example.seg2105_project_1_tutor_registration_form.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;

public final class SecurityUtils {

    private SecurityUtils() {}

    /** 16-byte random salt, returned as hex (32 chars). */
    public static String randomSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return toHex(b);
    }

    /** SHA-256 hash of the input string, returned as hex (64 chars). */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            return toHex(digest);
        } catch (Exception e) {
            return "";
        }
    }

    // --- helpers ---
    private static String toHex(byte[] data) {
        char[] hex = new char[data.length * 2];
        final char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xFF;
            hex[i * 2]     = digits[v >>> 4];
            hex[i * 2 + 1] = digits[v & 0x0F];
        }
        return new String(hex);
    }
}
