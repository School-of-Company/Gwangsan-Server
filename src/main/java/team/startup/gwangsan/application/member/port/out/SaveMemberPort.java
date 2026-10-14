package team.startup.gwangsan.application.member.port.out;

import team.startup.gwangsan.domain.member.model.Member;
import team.startup.gwangsan.domain.member.model.MemberDetail;

import java.util.Optional;

/** 순수 모델의 변경을 같은 트랜잭션의 저장 모델에 명시적으로 반영한다. */
public interface SaveMemberPort {

    Long register(Member member, MemberDetail detail);

    void save(Member member);

    void saveDetail(MemberDetail detail);

    /** 상세 엔티티 삭제. 기존 cascade(REMOVE) 로 회원도 함께 지워진다. */
    void deleteDetail(Long memberId);

    /** 같은 번호의 탈퇴 기록을 지우고 남긴 광산을 돌려준다. */
    Optional<Integer> removeWithdrawalRecord(String phoneNumber);
}
