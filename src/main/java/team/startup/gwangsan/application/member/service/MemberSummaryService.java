package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSummaryService implements MemberSummaryQuery {

    private final LoadMemberPort loadMemberPort;

    @Override
    public Optional<MemberSummary> findById(Long memberId) {
        return loadMemberPort.findSummaries(List.of(memberId)).stream().findFirst();
    }

    @Override
    public List<MemberSummary> findAllByIds(Collection<Long> memberIds) {
        return memberIds.isEmpty() ? List.of() : loadMemberPort.findSummaries(memberIds);
    }

    @Override
    public List<Long> findMemberIdsInPlace(Integer placeId) {
        return loadMemberPort.findMemberIdsInPlace(placeId);
    }
}
