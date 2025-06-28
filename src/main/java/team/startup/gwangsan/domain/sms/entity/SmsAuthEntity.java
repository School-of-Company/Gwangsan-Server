package team.startup.gwangsan.domain.sms.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash(value = "phone_authentication", timeToLive = 60L * 3)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsAuthEntity {

    @Id
    private String phone;

    @Indexed
    private String randomValue;

    private Boolean authentication;

    private Integer attemptCount;

    public void plusAttemptCount() {attemptCount++;}

    public void changeAuthentication() {authentication = true;}
}

