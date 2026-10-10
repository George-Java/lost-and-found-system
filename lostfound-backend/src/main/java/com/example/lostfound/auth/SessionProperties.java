package com.example.lostfound.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "lostfound.session")
public record SessionProperties(Duration ttl) {

    public SessionProperties {
        if (ttl == null) {
            ttl = Duration.ofHours(12);
        }
    }
}
