package com.example.lostfound.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 两级缓存：先查 JVM 进程内 Caffeine，未命中再查 Redis，仍未命中才回源数据库。
 * 两级都存 JSON 字符串，避免缓存对象被业务代码改坏，也便于 Canal 直接清理。
 */
@Component
public class TwoLevelCache {

    private static final Logger log = LoggerFactory.getLogger(TwoLevelCache.class);

    private final Cache<String, String> localCache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public TwoLevelCache(StringRedisTemplate redisTemplate,
                         ObjectMapper objectMapper,
                         @Value("${lostfound.cache.ttl:10m}") Duration ttl,
                         @Value("${lostfound.cache.local-max-size:10000}") long localMaxSize) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(localMaxSize)
                .expireAfterWrite(ttl)
                .build();
    }

    /** 缓存读取：Caffeine -> Redis -> loader(数据库)，命中下层会回填上层。 */
    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        return get(key, objectMapper.getTypeFactory().constructType(type), loader);
    }

    /** 缓存读取（列表）：Caffeine -> Redis -> loader(数据库)。 */
    public <T> List<T> getList(String key, Class<T> elementType, Supplier<List<T>> loader) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return get(key, type, loader);
    }

    /** 缓存读取：Caffeine -> Redis -> loader(数据库)，命中下层会回填上层。 */
    public <T> T get(String key, JavaType type, Supplier<T> loader) {
        String json = localCache.getIfPresent(key);
        if (json == null) {
            json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                localCache.put(key, json);
            }
        }
        if (json != null) {
            T cached = read(json, type);
            if (cached != null) {
                return cached;
            }
        }

        T value = loader.get();
        if (value != null) {
            put(key, value);
        }
        return value;
    }

    public void put(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            localCache.put(key, json);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception ex) {
            log.warn("写缓存失败, key={}", key, ex);
        }
    }

    public void evict(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
    }

    /** 按前缀清理，例如某个表变更后清掉它关联的所有缓存键。 */
    public void evictByPrefix(String prefix) {
        Set<String> keys = localCache.asMap().keySet();
        keys.removeIf(key -> key.startsWith(prefix));
        // 本项目缓存键总量很小，KEYS 足够；键规模变大时再换成 SCAN 游标删除
        Set<String> redisKeys = redisTemplate.keys(prefix + "*");
        if (redisKeys != null && !redisKeys.isEmpty()) {
            redisTemplate.delete(redisKeys);
        }
    }

    private <T> T read(String json, JavaType type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("缓存内容解析失败，按未命中处理: {}", json, ex);
            return null;
        }
    }
}
