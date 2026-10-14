package team.startup.gwangsan.adapter.persistence.notification;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import team.startup.gwangsan.domain.notification.model.OsType;

// 패키지 이동 전 저장된 hash 의 _class 값과 같게 유지한다. 이전 버전으로 롤백해도 새로 쓴 hash 를 그대로 읽는다.
@TypeAlias("team.startup.gwangsan.domain.notification.entity.DeviceToken")
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
