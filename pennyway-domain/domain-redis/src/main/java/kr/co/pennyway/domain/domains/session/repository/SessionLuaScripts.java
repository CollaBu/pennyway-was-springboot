package kr.co.pennyway.domain.domains.session.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@RequiredArgsConstructor
public enum SessionLuaScripts {
    SAVE(
            "redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]) " +
                    "return redis.call('HEXPIRE', KEYS[1], ARGV[3], 'FIELDS', '1', ARGV[1])",
            List.class
    ),
    FIND(
            "return redis.call('HGET', KEYS[1], ARGV[1])",
            String.class
    ),
    FIND_ALL(
            "return redis.call('HGETALL', KEYS[1])",
            List.class
    ),
    GET_TTL(
            "return redis.call('HTTL', KEYS[1], 'FIELDS', '1', ARGV[1])",
            Long.class
    ),
    EXISTS(
            "return redis.call('HEXISTS', KEYS[1], ARGV[1])",
            Boolean.class
    ),
    RESET_TTL(
            "return redis.call('HEXPIRE', KEYS[1], ARGV[2], 'FIELDS', '1', ARGV[1])",
            Long.class
    ),
    DELETE(
            "return redis.call('HDEL', KEYS[1], ARGV[1])",
            Long.class
    );

    private final String script;
    private final Class<?> returnType;

    public <T> RedisScript<T> getScript() {
        return RedisScript.of(script, (Class<T>) returnType);
    }

    public <T> Class<T> getReturnType() {
        return (Class<T>) returnType;
    }
}
