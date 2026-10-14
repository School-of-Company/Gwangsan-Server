package team.startup.gwangsan.application.trade.port.out;

/** 거래 저장소의 회원 참조 bulk 재지정. 각 호출 뒤 영속성 컨텍스트가 비워진다. */
public interface TradeMemberReferencePort {
    void reassignBuyer(Long memberId, Long dummyMemberId);

    void reassignSeller(Long memberId, Long dummyMemberId);

    void reassignCancelRequester(Long memberId, Long dummyMemberId);
}
