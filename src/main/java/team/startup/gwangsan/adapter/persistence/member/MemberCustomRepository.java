package team.startup.gwangsan.adapter.persistence.member;

import team.startup.gwangsan.adapter.web.post.dto.GetProductMemberResponse;

import java.util.Collection;
import java.util.List;

public interface MemberCustomRepository {
    List<GetProductMemberResponse> findProductMemberResponsesByMemberIds(Collection<Long> memberIds);
}
