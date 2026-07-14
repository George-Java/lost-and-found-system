package com.example.lostfound.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String dbPassword) {
        if (dbPassword == null || dbPassword.isBlank()) {
            return false;
        }
        if (isBcrypt(dbPassword)) {
            return ENCODER.matches(rawPassword, dbPassword);
        }
        return rawPassword.equals(dbPassword);
    }

    public static boolean needsRehash(String dbPassword) {
        return !isBcrypt(dbPassword);
    }

    public static boolean isBcrypt(String dbPassword) {
        return dbPassword != null
                && (dbPassword.startsWith("$2a$") || dbPassword.startsWith("$2b$") || dbPassword.startsWith("$2y$"));
    }
}
