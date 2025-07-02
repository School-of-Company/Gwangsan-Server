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

        memberRepository.findAll().forEach(m -> {
            System.out.println("memberId = " + m.getId());
            memberDetailRepository.findById(m.getId())
                    .ifPresentOrElse(
                            d -> System.out.println("✅ memberDetail found for id " + m.getId()),
                            () -> System.out.println("❌ memberDetail NOT found for id " + m.getId())
                    );
        });


        return memberRepository.findAll().stream()
                .map(member -> {
                    MemberDetail detail = memberDetailRepository.findById(member.getId())
                            .orElseThrow(NotFoundMemberDetailException::new);
                    return new FindAllUserInfoResponse(
                            member.getId(),
                            member.getNickname(),
                            detail.getProfileUrl(),
                            detail.getLight(),
                            detail.getGwangsan()
                    );
                })
                .toList();
    }
}

