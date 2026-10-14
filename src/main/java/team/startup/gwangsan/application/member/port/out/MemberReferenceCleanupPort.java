package team.startup.gwangsan.application.member.port.out;

/** 다른 업무가 가진 회원 참조 정리. 각 업무 공개 계약을 호출자 트랜잭션 안에서 실행한다. */
public interface MemberReferenceCleanupPort {

    /** 상품·예약자·거래·채팅·후기·신고·관리자 알림·철회·알림·공지의 회원 참조를 dummy 로 바꾼다. */
    void reassignReferences(Long memberId, Long dummyMemberId);

    /** 관심 키워드·정지·차단·알림 수신 기록을 즉시(bulk) 지운다. 이후 단계의 영속성 컨텍스트 clear 에 유실되면 안 된다. */
    void deletePersonalData(Long memberId);
}
