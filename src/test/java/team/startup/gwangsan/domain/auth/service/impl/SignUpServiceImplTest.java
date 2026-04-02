package team.startup.gwangsan.domain.auth.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import team.startup.gwangsan.domain.auth.exception.*;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignUpRequest;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.dong.repository.DongRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.repository.RelatedKeywordRepository;
import team.startup.gwangsan.global.redis.RedisUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignUpServiceImpl 단위 테스트")
class SignUpServiceImplTest {

    @InjectMocks
    private SignUpServiceImpl service;

    @Mock private MemberRepository memberRepository;
    @Mock private RedisUtil redisUtil;
    @Mock private DongRepository dongRepository;
    @Mock private PlaceRepository placeRepository;
    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RelatedKeywordRepository relatedKeywordRepository;
    @Mock private MemberRelatedKeywordRepository memberRelatedKeywordRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private SignUpRequest validRequest() {
        return new SignUpRequest(
                "홍길동", "테스터일", "password", "01012345678",
                "광산동", 1, List.of("Java", "Spring"), "추천인닉", "자기소개"
        );
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("회원과 상세정보를 저장하고 이벤트를 발행한다")
            void it_saves_member_and_publishes_events() {
                SignUpRequest request = validRequest();

                when(memberRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
                when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);
                when(redisUtil.get("sms:verified:" + request.phoneNumber(), Boolean.class)).thenReturn(true);
                when(dongRepository.findByName(request.dongName())).thenReturn(Optional.of(mock(Dong.class)));
                when(placeRepository.findById(request.placeId())).thenReturn(Optional.of(mock(Place.class)));
                when(memberRepository.findByNickname(request.recommender())).thenReturn(Optional.of(mock(Member.class)));
                when(passwordEncoder.encode(any())).thenReturn("encoded");
                when(relatedKeywordRepository.findByName(any())).thenReturn(Optional.empty());
                when(relatedKeywordRepository.save(any())).thenReturn(mock(RelatedKeyword.class));
                when(memberRepository.save(any())).thenReturn(mock(Member.class));

                service.execute(request);

                verify(memberRepository).save(any(Member.class));
                verify(memberDetailRepository).save(any());
                verify(applicationEventPublisher, times(2)).publishEvent(any(Object.class));
                verify(redisUtil).delete("sms:verified:" + request.phoneNumber());
            }
        }

        @Nested
        @DisplayName("전화번호가 중복일 때")
        class Context_with_duplicate_phone {

            @Test
            @DisplayName("DuplicatePhoneNumberException을 던진다")
            void it_throws_duplicate_phone_exception() {
                SignUpRequest request = validRequest();
                when(memberRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(true);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(DuplicatePhoneNumberException.class);
            }
        }

        @Nested
        @DisplayName("닉네임이 중복일 때")
        class Context_with_duplicate_nickname {

            @Test
            @DisplayName("DuplicateNicknameException을 던진다")
            void it_throws_duplicate_nickname_exception() {
                SignUpRequest request = validRequest();
                when(memberRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
                when(memberRepository.existsByNickname(request.nickname())).thenReturn(true);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(DuplicateNicknameException.class);
            }
        }

        @Nested
        @DisplayName("SMS 인증 정보가 Redis에 없을 때")
        class Context_with_sms_auth_not_found {

            @Test
            @DisplayName("SmsAuthNotFoundException을 던진다")
            void it_throws_sms_auth_not_found_exception() {
                SignUpRequest request = validRequest();
                when(memberRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
                when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);
                when(redisUtil.get("sms:verified:" + request.phoneNumber(), Boolean.class)).thenReturn(null);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(SmsAuthNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("SMS 인증이 완료되지 않았을 때")
        class Context_with_sms_not_completed {

            @Test
            @DisplayName("SmsAuthNotCompletedException을 던진다")
            void it_throws_sms_not_completed_exception() {
                SignUpRequest request = validRequest();
                when(memberRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
                when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);
                when(redisUtil.get("sms:verified:" + request.phoneNumber(), Boolean.class)).thenReturn(false);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(SmsAuthNotCompletedException.class);
            }
        }

        @Nested
        @DisplayName("동 정보가 없을 때")
        class Context_with_dong_not_found {

            @Test
            @DisplayName("DongNotFoundException을 던진다")
            void it_throws_dong_not_found_exception() {
                SignUpRequest request = validRequest();
                when(memberRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
                when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);
                when(redisUtil.get("sms:verified:" + request.phoneNumber(), Boolean.class)).thenReturn(true);
                when(dongRepository.findByName(request.dongName())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(DongNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("지점이 없을 때")
        class Context_with_place_not_found {

            @Test
            @DisplayName("PlaceNotFoundException을 던진다")
            void it_throws_place_not_found_exception() {
                SignUpRequest request = validRequest();
                when(memberRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
                when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);
                when(redisUtil.get("sms:verified:" + request.phoneNumber(), Boolean.class)).thenReturn(true);
                when(dongRepository.findByName(request.dongName())).thenReturn(Optional.of(mock(Dong.class)));
                when(placeRepository.findById(request.placeId())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(PlaceNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("추천인이 없을 때")
        class Context_with_recommender_not_found {

            @Test
            @DisplayName("RecommenderNotFoundException을 던진다")
            void it_throws_recommender_not_found_exception() {
                SignUpRequest request = validRequest();
                when(memberRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
                when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);
                when(redisUtil.get("sms:verified:" + request.phoneNumber(), Boolean.class)).thenReturn(true);
                when(dongRepository.findByName(request.dongName())).thenReturn(Optional.of(mock(Dong.class)));
                when(placeRepository.findById(request.placeId())).thenReturn(Optional.of(mock(Place.class)));
                when(memberRepository.findByNickname(request.recommender())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(RecommenderNotFoundException.class);
            }
        }
    }
}
