package team.startup.gwangsan.adapter.persistence.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.port.out.RefreshTokenPort;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void save(String phoneNumber, String refreshToken) {
        refreshTokenRepository.save(RefreshToken.builder().phoneNumber(phoneNumber).token(refreshToken).build());
    }

    @Override
    public Optional<String> findPhoneNumberByToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken).map(RefreshToken::getPhoneNumber);
    }

    @Override
    public void deleteByPhoneNumber(String phoneNumber) {
        refreshTokenRepository.deleteById(phoneNumber);
    }
}
