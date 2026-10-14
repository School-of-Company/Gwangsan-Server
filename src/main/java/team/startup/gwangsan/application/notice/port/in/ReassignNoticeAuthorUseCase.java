package team.startup.gwangsan.application.notice.port.in;

/** 회원 탈퇴 시 공지 작성자를 dummy 회원으로 바꾼다. 호출자 트랜잭션에 참여한다. */
public interface ReassignNoticeAuthorUseCase {
    void reassign(Long memberId, Long dummyMemberId);
}
