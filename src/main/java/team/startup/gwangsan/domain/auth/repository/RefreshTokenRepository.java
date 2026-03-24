package team.startup.gwangsan.domain.auth.repository;

import org.springframework.data.repository.CrudRepository;
import team.startup.gwangsan.domain.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
}

