package team.startup.gwangsan.application.notification.port.out;

import java.util.Collection;
import java.util.List;

public interface LoadPushTokensPort {
    /** 회원 순서대로 각 회원의 기기 토큰을 모은다. */
    List<String> loadTokens(Collection<Long> memberIds);
}
