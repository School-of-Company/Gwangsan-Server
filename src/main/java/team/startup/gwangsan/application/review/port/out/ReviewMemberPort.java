package team.startup.gwangsan.application.review.port.out;

/** 회원(member 소유). 없으면 회원 소유자의 기존 오류. */
public interface ReviewMemberPort {
    String findNickname(long memberId);

    /** 대상 회원의 빛(light) 점수를 더한다. 호출자 트랜잭션에 참여한다. */
    void addLight(long memberId, Integer light);
}
