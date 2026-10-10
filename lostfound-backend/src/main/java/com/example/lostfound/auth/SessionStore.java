package com.example.lostfound.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/** Redis-backed sessions survive process restarts and allow future horizontal scaling. */
@Component
public class SessionStore {
    private static final String KEY_PREFIX = "lostfound:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration sessionTtl;

    public SessionStore(StringRedisTemplate redisTemplate,
                        ObjectMapper objectMapper,
                        SessionProperties sessionProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sessionTtl = sessionProperties.ttl();
    }

    public String create(SessionUser sessionUser) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + token, objectMapper.writeValueAsString(sessionUser), sessionTtl);
            return token;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to create session", ex);
        }
    }

    public SessionUser get(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (value == null) {
            return null;
        }
        try {
            // Sliding expiration prevents active users from being logged out unexpectedly.
            redisTemplate.expire(KEY_PREFIX + token, sessionTtl);
            return objectMapper.readValue(value, SessionUser.class);
        } catch (JsonProcessingException ex) {
            remove(token);
            return null;
        }
    }

    public void remove(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(KEY_PREFIX + token);
        }
    }
}
