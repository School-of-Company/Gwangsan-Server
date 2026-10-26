package team.startup.gwangsan.application.member.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import team.startup.gwangsan.application.member.port.in.AddMemberLightUseCase;
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.application.member.port.in.ManageMemberUseCase;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.GwangsanBalancePort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** 실제 Spring 트랜잭션 프록시로 잔액 command 의 MANDATORY 참여 규칙을 확인한다(DB 없이 H2 메모리 커넥션). */
@DisplayName("member command 트랜잭션 경계 (ChangeGwangsan·AddMemberLight·ManageMember)")
class ChangeGwangsanTransactionTest {

    private static final LoadMemberPort LOAD = mock(LoadMemberPort.class);
    private static final SaveMemberPort SAVE = mock(SaveMemberPort.class);
    private static final GwangsanBalancePort BALANCE = mock(GwangsanBalancePort.class);

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new DataSourceTransactionManager(new DriverManagerDataSource("jdbc:h2:mem:change-gwangsan"));
        }

        @Bean
        ChangeGwangsanService changeGwangsanService() {
            return new ChangeGwangsanService(BALANCE);
        }

        @Bean
        AddMemberLightService addMemberLightService() {
            return new AddMemberLightService(LOAD, SAVE);
        }

        @Bean
        ManageMemberService manageMemberService() {
            return new ManageMemberService(LOAD, SAVE);
        }
    }

    private AnnotationConfigApplicationContext context;
    private ChangeGwangsanUseCase useCase;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        reset(LOAD, SAVE, BALANCE);
        context = new AnnotationConfigApplicationContext(Config.class);
        useCase = context.getBean(ChangeGwangsanUseCase.class);
        transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Nested
    @DisplayName("호출자 트랜잭션이 없으면")
    class Context_without_transaction {

        @Test
        @DisplayName("it_잔액을 읽거나 저장하지 않고 거절한다")
        void it_rejects_standalone_call() {
            assertThatThrownBy(() -> useCase.credit(1L, 100, GwangsanLedgerType.SIGNUP_REWARD, 1L))
                    .isInstanceOf(IllegalTransactionStateException.class);
            verifyNoInteractions(LOAD, SAVE, BALANCE);
        }

        @Test
        @DisplayName("it_빛 가산·상태·역할·가입 기각·탈퇴 기록 회수 command 도 단독 호출을 거절한다")
        void it_rejects_standalone_member_commands() {
            AddMemberLightUseCase light = context.getBean(AddMemberLightUseCase.class);
            ManageMemberUseCase manage = context.getBean(ManageMemberUseCase.class);

            assertThatThrownBy(() -> light.execute(1L, 1)).isInstanceOf(IllegalTransactionStateException.class);
            assertThatThrownBy(() -> manage.changeStatus(1L, MemberStatus.SUSPENDED)).isInstanceOf(IllegalTransactionStateException.class);
            assertThatThrownBy(() -> manage.changeRole(1L, MemberRole.ROLE_USER, 1)).isInstanceOf(IllegalTransactionStateException.class);
            assertThatThrownBy(() -> manage.rejectSignUp(1L)).isInstanceOf(IllegalTransactionStateException.class);
            assertThatThrownBy(() -> manage.takeWithdrawnGwangsan("01000000000")).isInstanceOf(IllegalTransactionStateException.class);
            verifyNoInteractions(LOAD, SAVE);
        }
    }

    @Nested
    @DisplayName("호출자 트랜잭션 안에서는")
    class Context_within_transaction {

        @Test
        @DisplayName("it_같은 트랜잭션에 참여해 변경을 저장한다")
        void it_joins_caller_transaction() {
            when(BALANCE.add(1L, -30)).thenReturn(70);

            transaction.executeWithoutResult(status -> useCase.credit(1L, -30, GwangsanLedgerType.ADMIN_ADJUST, 1L));

            verify(BALANCE).record(1L, -30, 70, GwangsanLedgerType.ADMIN_ADJUST, 1L);
        }

        @Test
        @DisplayName("it_command 가 실패하면 호출자와 공유한 트랜잭션 전체를 rollback-only 로 만들어 커밋을 막는다")
        void it_marks_shared_transaction_rollback_only() {
            when(BALANCE.add(1L, 30)).thenThrow(new NotFoundMemberDetailException());

            boolean[] rollbackOnly = new boolean[1];
            // 호출자가 예외를 잡고 커밋하려 해도 공유 트랜잭션은 커밋되지 않는다.
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                assertThatThrownBy(() -> useCase.credit(1L, 30, GwangsanLedgerType.SIGNUP_REWARD, 1L))
                        .isInstanceOf(NotFoundMemberDetailException.class);
                rollbackOnly[0] = status.isRollbackOnly();
            })).isInstanceOf(UnexpectedRollbackException.class);

            assertThat(rollbackOnly[0]).isTrue();
            verify(BALANCE, never()).record(anyLong(), anyInt(), anyInt(), any(), any());
        }
    }
}
