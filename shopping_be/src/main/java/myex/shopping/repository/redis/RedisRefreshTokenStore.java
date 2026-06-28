package myex.shopping.repository.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import myex.shopping.dto.authdto.RefreshTokenSession;
import myex.shopping.repository.RefreshTokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(String refreshToken, RefreshTokenSession session, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(toKey(refreshToken), objectMapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize refresh token session.", ex);
        }
    }

    @Override
    public Optional<RefreshTokenSession> findByRefreshToken(String refreshToken) {
        String value = stringRedisTemplate.opsForValue().get(toKey(refreshToken));
        return deserialize(value);
    }

    @Override
    public Optional<RefreshTokenSession> consume(String refreshToken) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(toKey(refreshToken));
        return deserialize(value);
    }

    @Override
    public void delete(String refreshToken) {
        stringRedisTemplate.delete(toKey(refreshToken));
    }

    private String toKey(String refreshToken) {
        return KEY_PREFIX + hash(refreshToken);
    }

    private Optional<RefreshTokenSession> deserialize(String value) {
        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, RefreshTokenSession.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize refresh token session.", ex);
        }
    }

    private String hash(String refreshToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", ex);
        }
    }
}
