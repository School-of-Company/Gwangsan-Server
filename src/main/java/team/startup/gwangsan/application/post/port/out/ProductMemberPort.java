package team.startup.gwangsan.application.post.port.out;

import team.startup.gwangsan.application.post.data.AuthorProfile;

import java.util.Collection;
import java.util.Map;

public interface ProductMemberPort {
    /** 회원의 지점 ID. 회원 상세가 없으면 예외. */
    Integer placeIdOf(Long memberId);

    /** 회원 상세가 없으면 예외. */
    AuthorProfile profile(Long memberId);

    Map<Long, AuthorProfile> profiles(Collection<Long> memberIds);

    String nicknameOf(Long memberId);
}
