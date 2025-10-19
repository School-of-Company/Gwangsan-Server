package team.startup.gwangsan.domain.notification.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import team.startup.gwangsan.domain.notification.entity.constant.OsType;

@RedisHash(value = "gwangsan_deviceToken", timeToLive = 60L * 60 * 24 * 30)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    private String deviceId;

    @Indexed
    private Long userId;

    private String deviceToken;

    private OsType osType;
}