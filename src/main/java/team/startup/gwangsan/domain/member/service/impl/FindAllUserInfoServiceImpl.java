package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindAllUserInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.service.FindAllUserInfoService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAllUserInfoServiceImpl implements FindAllUserInfoService {

    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FindAllUserInfoResponse> execute() {

        return memberDetailRepository.findAllWithMember().stream()
                .map(detail -> {
                    return new FindAllUserInfoResponse(
                            detail.getMember().getId(),
                            detail.getMember().getNickname(),
                            detail.getProfileUrl(),
                            detail.getLight(),
                            detail.getGwangsan()
                    );
                })
                .toList();
    }
}
