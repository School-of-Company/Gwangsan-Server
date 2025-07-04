package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.DuplicateNicknameException;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.request.UpdateMyInfoRequest;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.service.UpdateMyInfoService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class UpdateMyInfoServiceImpl implements UpdateMyInfoService {

    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(UpdateMyInfoRequest request) {
        Member member = memberUtil.getCurrentMember();

        if (!member.getNickname().equals(request.nickname())
                && memberRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException();
        }

        member.updateNickname(request.nickname());

        MemberDetail detail = memberDetailRepository.findById(member.getId())
                .orElseThrow(NotFoundMemberException::new);

        detail.updateProfileUrl(request.profileUrl());
    }
}