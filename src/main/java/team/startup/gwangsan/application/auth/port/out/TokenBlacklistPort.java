package team.startup.gwangsan.application.auth.port.out;

public interface TokenBlacklistPort {

    void add(String accessToken, long expirationMillis);
}
