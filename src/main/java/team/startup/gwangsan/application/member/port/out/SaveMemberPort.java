package team.startup.gwangsan.application.member.port.out;

import team.startup.gwangsan.domain.member.model.Member;
import team.startup.gwangsan.domain.member.model.MemberDetail;

/** 순수 모델의 변경을 같은 트랜잭션의 저장 모델에 명시적으로 반영한다. */
public interface SaveMemberPort {

    Long register(Member member, MemberDetail detail);

    void save(Member member);

    void saveDetail(MemberDetail detail);
}
