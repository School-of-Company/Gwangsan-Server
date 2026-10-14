package team.startup.gwangsan.adapter.persistence.auth;

import org.springframework.data.repository.CrudRepository;
import team.startup.gwangsan.adapter.persistence.auth.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
}

