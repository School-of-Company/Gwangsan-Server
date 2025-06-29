package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.*;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignUpRequest;
import team.startup.gwangsan.domain.auth.service.SignUpService;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.dong.repository.DongRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.repository.RelatedKeywordRepository;
import team.startup.gwangsan.domain.sms.entity.SmsAuthEntity;
import team.startup.gwangsan.domain.sms.repository.SmsAuthRepository;


@Service
@RequiredArgsConstructor
public class SignUpServiceImpl implements SignUpService {

    private final MemberRepository memberRepository;
    private final SmsAuthRepository smsAuthRepository;
    private final DongRepository dongRepository;
    private final PlaceRepository placeRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final PasswordEncoder passwordEncoder;
    private final RelatedKeywordRepository relatedKeywordRepository;
    private final MemberRelatedKeywordRepository memberRelatedKeywordRepository;

    @Override
    @Transactional
    public void execute(SignUpRequest request) {
        validateDuplicatePhoneNumber(request.phoneNumber());
        validateDuplicateNickname(request.nickname());

        SmsAuthEntity smsAuthEntity = smsAuthRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(SmsAuthNotFoundException::new);

        validateSmsAuthentication(smsAuthEntity);

        Dong dong = dongRepository.findById(request.dongId())
                .orElseThrow(DongNotFoundException::new);

        Place place = placeRepository.findById(request.placeId())
                .orElseThrow(PlaceNotFoundException::new);

        Member recommender = memberRepository.findByNickname(request.recommender())
                .orElseThrow(RecommenderNotFoundException::new);

        Member member = Member.builder()
                .name(request.name())
                .nickname(request.nickname())
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber())
                .recommender(recommender)
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.PENDING)
                .build();

        memberRepository.save(member);

        MemberDetail memberDetail = MemberDetail.builder()
                .member(member)
                .dong(dong)
                .place(place)
                .gwangsan(0)
                .light(1)
                .profileUrl(null)
                .build();

        memberDetailRepository.save(memberDetail);

        for (String keywordContent : request.specialties()) {
            RelatedKeyword keyword = relatedKeywordRepository.findByName(keywordContent)
                    .orElseGet(() -> relatedKeywordRepository.save(
                            RelatedKeyword.builder()
                                    .name(keywordContent)
                                    .build()
                    ));

            MemberRelatedKeyword mapping = MemberRelatedKeyword.builder()
                    .member(member)
                    .relatedKeyword(keyword)
                    .build();

            memberRelatedKeywordRepository.save(mapping);
        }
    }

    private void validateDuplicatePhoneNumber(String phoneNumber) {
        if (memberRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicatePhoneNumberException();
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new DuplicateNicknameException();
        }
    }

    private void validateSmsAuthentication(SmsAuthEntity smsAuthEntity) {
        if (!smsAuthEntity.getAuthentication()) {
            throw new SmsAuthNotCompletedException();
        }
    }
}
