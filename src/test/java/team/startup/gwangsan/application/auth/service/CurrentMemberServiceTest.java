package team.startup.gwangsan.application.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.auth.data.CurrentMemberResult;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.domain.auth.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.account;

@DisplayName("CurrentMemberService 단위 테스트")
class CurrentMemberServiceTest {

    @Nested
    @DisplayName("query() 메서드는")
    class Describe_query {

        @Test
        @DisplayName("현재 회원의 id와 nickname을 반환한다")
        void it_returns_member_info() {
            AccountPort accountPort = mock(AccountPort.class);
            when(accountPort.findById(1L)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));

            CurrentMemberResult result = new CurrentMemberService(accountPort)
                    .query(new ActorContext(1L, ActorContext.Role.ROLE_USER));

            assertThat(result).isEqualTo(new CurrentMemberResult(1L, "테스터일"));
        }
    }
}
