package team.startup.gwangsan.compatibility;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import team.startup.gwangsan.architecture.ProductionClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사용자 알림함 저장 범위 (계획 "1차 구현 검토 확정: 알림 저장 범위") 의 발행자 전수 대조.
 * - OTHER_MEMBER_TRADE_COMPLETE(거래 완료 요청), TRADE_CANCEL(철회 완료) 만 업무와 같은 트랜잭션에 동기 저장한다 (PR07).
 * - 그 밖의 종류는 기존 AFTER_COMMIT 비동기(CreateAlertEvent / CreateAlertMembersEvent)를 유지한다. 발행 위치가 줄면 누락이다.
 * - TRADE_COMPLETE 알림함 기록은 기존 발행자가 없으며 새로 만들지 않는다.
 * 이벤트/enum 은 이동될 수 있으므로 단순 이름으로 찾고, 발행 위치는 "이벤트 생성 코드 단위 + 그 안에서 읽는 AlertType 상수" 로 센다.
 */
@DisplayName("사용자 알림 발행 범위 호환")
class AlertPublisherCompatibilityTest {

    static final Set<String> ATOMIC = Set.of("OTHER_MEMBER_TRADE_COMPLETE", "TRADE_CANCEL");
    // baseline develop 474a2cde 의 비동기 발행 위치 수 (원자 저장 대상 제외). 줄면 누락, 늘면 범위 확대.
    private static final Map<String, Long> ASYNC_BASELINE = Map.of(
            "RECOMMENDER", 2L,          // 가입(SignUp), 관리자 가입 승인(VerificationSignUp)
            "REPORT", 1L,               // 정지(SuspendMember)
            "REPORT_REJECT", 1L,        // 관리자 신고 기각(RejectAdminAlert)
            "TRADE_CANCEL_REJECT", 1L,  // 관리자 철회 기각(RejectAdminAlert)
            "REVIEW", 1L);              // 후기(CreateReview)
    // 이름으로 AlertType 에 1:1 매핑되는, 검토를 마친 dispatcher 파라미터 enum (PR11 application/admin/data).
    private static final Set<String> DISPATCH_ENUMS = Set.of("MemberAlertKind");
    private static final Map<String, Long> ASYNC_MEMBERS_BASELINE = Map.of("NOTICE", 1L); // 공지(CreateNotice)

    static final JavaClasses PRODUCTION = ProductionClasses.ALL;

    @Nested
    @DisplayName("CreateAlertEvent 비동기 발행은")
    class Describe_async_alert {

        @Test
        @DisplayName("it_원자 저장 대상이 아닌 종류의 발행 위치를 baseline 그대로 유지한다")
        void it_keeps_async_publishers() {
            Map<String, Long> withoutAtomic = new TreeMap<>(publishedTypes("CreateAlertEvent"));
            withoutAtomic.keySet().removeAll(ATOMIC);

            assertThat(withoutAtomic).isEqualTo(new TreeMap<>(ASYNC_BASELINE));
        }

        @Test
        @DisplayName("it_TRADE_COMPLETE 알림함 기록을 새로 발행하지 않는다")
        void it_does_not_publish_trade_complete() {
            assertThat(publishedTypes("CreateAlertEvent")).doesNotContainKey("TRADE_COMPLETE");
            assertThat(publishedTypes("CreateAlertMembersEvent")).doesNotContainKey("TRADE_COMPLETE");
        }
    }

    @Nested
    @DisplayName("PR07 전환 완료 후에는")
    class Describe_after_pr07 {

        @Test
        @DisplayName("it_원자 저장 대상 두 종류를 비동기로 다시 발행하지 않는다 (중복 저장 방지)")
        void it_does_not_publish_atomic_types_async() {
            assertThat(publishedTypes("CreateAlertEvent")).doesNotContainKeys(ATOMIC.toArray(String[]::new));
        }
    }

    @Nested
    @DisplayName("CreateAlertMembersEvent 비동기 발행은")
    class Describe_async_members_alert {

        @Test
        @DisplayName("it_공지 발행 위치를 baseline 그대로 유지한다")
        void it_keeps_notice_publisher() {
            assertThat(publishedTypes("CreateAlertMembersEvent")).isEqualTo(ASYNC_MEMBERS_BASELINE);
        }
    }

    /**
     * eventSimpleName 을 생성하는 코드 단위마다 그 안에서 읽는 사용자 AlertType 상수를 센다.
     * 관리자 알림(admin AlertType)과 섞이지 않도록 이벤트 생성자 파라미터의 enum 타입으로 거른다.
     * AlertType 을 상수가 아닌 변수로 넘기는 발행 위치는 대조할 수 없으므로 "?" 로 보고해 실패시킨다.
     */
    static Map<String, Long> publishedTypes(String eventSimpleName) {
        List<JavaClass> events = PRODUCTION.stream().filter(c -> c.getSimpleName().equals(eventSimpleName)).toList();
        assertThat(events).as(eventSimpleName + " 는 하나여야 한다").hasSize(1);
        JavaClass event = events.get(0);
        String alertType = event.getConstructors().stream()
                .flatMap(ctor -> ctor.getRawParameterTypes().stream())
                .filter(type -> type.getSimpleName().equals("AlertType"))
                .map(JavaClass::getName)
                .findFirst()
                .orElseThrow();

        return PRODUCTION.stream()
                .filter(c -> !c.equals(event))
                .flatMap(c -> c.getCodeUnits().stream())
                .filter(unit -> unit.getConstructorCallsFromSelf().stream()
                        .anyMatch(call -> call.getTargetOwner().equals(event)))
                .flatMap(unit -> alertTypesRead(unit, alertType))
                .collect(Collectors.groupingBy(t -> t, TreeMap::new, Collectors.counting()));
    }

    /**
     * AlertType 을 변수로 넘기는 발행 위치는 기본적으로 대조 불가("?")다.
     * 예외: 파라미터가 DISPATCH_ENUMS 의 enum 이고 이름으로 AlertType 에 1:1 매핑하는 dispatcher
     * (PR11 AdminNotificationBridge: AlertType.valueOf(kind.name())). 이때는 그 enum 상수를 읽는 호출 위치를 발행 위치로 센다.
     */
    private static Stream<String> alertTypesRead(JavaCodeUnit unit, String alertType) {
        List<String> read = unit.getFieldAccesses().stream()
                .filter(access -> access.getTargetOwner().getName().equals(alertType))
                .map(access -> access.getTarget().getName())
                .toList();
        if (!read.isEmpty()) {
            return read.stream();
        }
        List<JavaClass> dispatchEnums = unit.getRawParameterTypes().stream()
                .filter(type -> DISPATCH_ENUMS.contains(type.getSimpleName()))
                .toList();
        if (dispatchEnums.size() != 1) {
            return Stream.of("? " + unit.getFullName());
        }
        JavaClass kind = dispatchEnums.get(0);
        return PRODUCTION.stream()
                .filter(c -> !c.equals(kind) && !c.getName().matches(".*\\$\\d+$")) // enum 자체·switch 합성 클래스 제외
                .flatMap(c -> c.getCodeUnits().stream())
                .flatMap(caller -> caller.getFieldAccesses().stream())
                .filter(access -> access.getTargetOwner().equals(kind))
                .map(access -> access.getTarget().getName());
    }
}
