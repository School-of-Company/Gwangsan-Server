package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
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
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.redis.RedisUtil;

@Service
@RequiredArgsConstructor
public class SignUpServiceImpl implements SignUpService {

    private final MemberRepository memberRepository;
    private static final String VERIFIED_KEY_PREFIX = "sms:verified:";

    private final RedisUtil redisUtil;
    private final DongRepository dongRepository;
    private final PlaceRepository placeRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final PasswordEncoder passwordEncoder;
    private final RelatedKeywordRepository relatedKeywordRepository;
    private final MemberRelatedKeywordRepository memberRelatedKeywordRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void execute(SignUpRequest request) {
        validateDuplicatePhoneNumber(request.phoneNumber());
        validateDuplicateNickname(request.nickname());

        String verifiedKey = VERIFIED_KEY_PREFIX + request.phoneNumber();
        Boolean verified = redisUtil.get(verifiedKey, Boolean.class);
        if (verified == null) {
            throw new SmsAuthNotFoundException();
        }

        validateSmsAuthentication(verified);

        Dong dong = dongRepository.findByName(request.dongName())
                .orElseThrow(DongNotFoundException::new);

        Place place = placeRepository.findByName(request.placeName())
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
                .description(request.description())
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
        applicationEventPublisher.publishEvent(new CreateAdminAlertEvent(
                AlertType.SIGN_UP,
                member.getId(),
                member.getId()
        ));

        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                member.getId(),
                recommender.getId(),
                team.startup.gwangsan.domain.alert.entity.constant.AlertType.RECOMMENDER));

        redisUtil.delete(verifiedKey);
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

    private void validateSmsAuthentication(Boolean verified) {
        if (!Boolean.TRUE.equals(verified)) {
            throw new SmsAuthNotCompletedException();
        }
    }
}
