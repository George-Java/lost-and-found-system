package com.example.lostfound.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {
    private final Map<String, SessionUser> tokenStore = new ConcurrentHashMap<>();

    public String create(SessionUser sessionUser) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, sessionUser);
        return token;
    }

    public SessionUser get(String token) {
        return tokenStore.get(token);
    }
}
