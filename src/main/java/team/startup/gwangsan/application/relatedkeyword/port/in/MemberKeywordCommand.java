package team.startup.gwangsan.application.relatedkeyword.port.in;

import java.util.List;

/** 다른 업무(회원 가입·내 정보 수정)가 쓰는 회원 관심 키워드 변경. */
public interface MemberKeywordCommand {

    /** 이름마다 키워드를 찾거나 만들고 회원과 연결한다. */
    void add(Long memberId, List<String> names);

    /** 기존 연결을 파생 삭제로 지우고 다시 연결한다. 같은 컨텍스트의 dirty 변경을 보존한다. */
    void replace(Long memberId, List<String> names);
}
