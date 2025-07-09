package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindAllUserInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.service.FindAllUserInfoService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAllUserInfoServiceImpl implements FindAllUserInfoService {

    private final MemberDetailRepository memberDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FindAllUserInfoResponse> execute() {
        return memberDetailRepository.findAllWithMember().stream()
                .map(detail -> {
                    Member member = detail.getMember();
                    return new FindAllUserInfoResponse(
                            member.getId(),
                            member.getNickname(),
                            detail.getProfileUrl(),
                            detail.getLight(),
                            detail.getGwangsan(),
                            member.getPhoneNumber(),
                            member.getRole(),
                            member.getStatus(),
                            member.getJoinedAt()
                    );
                })
                .toList();
    }
}
