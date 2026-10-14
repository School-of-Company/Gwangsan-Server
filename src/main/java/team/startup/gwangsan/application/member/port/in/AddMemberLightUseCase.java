package team.startup.gwangsan.application.member.port.in;

/** 회원 빛 가산 공개 command. 후기 저장과 함께 커밋되도록 호출자 트랜잭션이 필수다. 결과는 0~100 으로 맞춘다. */
public interface AddMemberLightUseCase {

    /** 상세가 없으면 NotFoundMemberDetail. */
    void execute(Long memberId, Integer delta);
}
