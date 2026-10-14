package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.RegisterMemberCommand;

/** 가입 신청 회원 생성. 호출자 트랜잭션에 참여하며 생성된 회원 ID 를 반환한다. */
public interface RegisterMemberUseCase {

    Long execute(RegisterMemberCommand command);
}
