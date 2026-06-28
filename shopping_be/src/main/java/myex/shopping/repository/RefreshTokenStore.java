package myex.shopping.repository;

import myex.shopping.dto.authdto.RefreshTokenSession;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStore {

    void save(String refreshToken, RefreshTokenSession session, Duration ttl);

    Optional<RefreshTokenSession> findByRefreshToken(String refreshToken);

    Optional<RefreshTokenSession> consume(String refreshToken);

    void delete(String refreshToken);
}
