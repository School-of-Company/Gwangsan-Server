package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.*;
import team.startup.gwangsan.domain.auth.presentation.dto.SignUpRequest;
import team.startup.gwangsan.domain.auth.service.SignUpService;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.dong.repository.DongRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.sms.entity.SmsAuthEntity;
import team.startup.gwangsan.domain.sms.repository.SmsAuthRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class SignUpServiceImpl implements SignUpService {

    private final MemberRepository memberRepository;
    private final SmsAuthRepository smsAuthRepository;
    private final DongRepository dongRepository;
    private final PlaceRepository placeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void execute(SignUpRequest request) {
        if (memberRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicatePhoneNumberException();
        }

        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new DuplicateNicknameException();
        }

        SmsAuthEntity smsAuthEntity = smsAuthRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(SmsAuthNotFoundException::new);

        if (!smsAuthEntity.getAuthentication()) {
            throw new SmsAuthNotCompletedException();
        }

        Dong dong = dongRepository.findById(request.getDongId())
                .orElseThrow(DongNotFoundException::new);

        Place place = placeRepository.findById(request.getPlaceId())
                .orElseThrow(PlaceNotFoundException::new);

        Member recommender = memberRepository.findByNickname(request.getRecommender())
                .orElseThrow(RecommenderNotFoundException::new);

        Member member = Member.builder()
                .name(request.getName())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .dong(dong)
                .place(place)
                .recommender(recommender)
                .specialty(request.getSpecialty())
                .build();

        memberRepository.save(member);
    }
}

