package team.startup.gwangsan.adapter.persistence.member;

import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductMemberResponse;

import java.util.Collection;
import java.util.List;

public interface MemberCustomRepository {
    List<GetProductMemberResponse> findProductMemberResponsesByMemberIds(Collection<Long> memberIds);
}
