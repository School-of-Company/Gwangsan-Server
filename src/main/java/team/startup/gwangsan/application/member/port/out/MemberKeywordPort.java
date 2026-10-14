package team.startup.gwangsan.application.member.port.out;

import java.util.List;

/** 회원 관심 키워드(특기). relatedkeyword 업무 소유 데이터다. */
public interface MemberKeywordPort {

    List<String> findNames(Long memberId);

    /** 이름마다 키워드를 찾거나 만들고 회원과 연결한다. */
    void add(Long memberId, List<String> names);

    /** 기존 연결을 모두 지우고 다시 연결한다. */
    void replace(Long memberId, List<String> names);
}
