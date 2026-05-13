package com.example.lostfound.auth;

public class AuthContext {
    private static final ThreadLocal<SessionUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(SessionUser sessionUser) {
        CURRENT.set(sessionUser);
    }

    public static SessionUser get() {
        return CURRENT.get();
    }

    public static Long userId() {
        SessionUser user = CURRENT.get();
        return user == null ? null : user.getUserId();
    }

    public static String role() {
        SessionUser user = CURRENT.get();
        return user == null ? null : user.getRole();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
