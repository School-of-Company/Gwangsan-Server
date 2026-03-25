package team.startup.gwangsan.domain.member.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.auth.exception.DuplicateNicknameException;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.request.UpdateMyInfoRequest;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.repository.RelatedKeywordRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMyInfoServiceImpl 단위 테스트")
class UpdateMyInfoServiceImplTest {

    @InjectMocks
    private UpdateMyInfoServiceImpl service;

    @Mock private MemberRepository memberRepository;
    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private RelatedKeywordRepository relatedKeywordRepository;
    @Mock private MemberRelatedKeywordRepository memberRelatedKeywordRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("닉네임이 변경되지 않은 경우")
        class Context_with_same_nickname {

            @Test
            @DisplayName("중복 체크 없이 업데이트한다")
            void it_updates_without_duplicate_check() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(member.getNickname()).thenReturn("테스터일");

                MemberDetail detail = mock(MemberDetail.class);
                UpdateMyInfoRequest request = new UpdateMyInfoRequest("테스터일", "새 설명", List.of("Java"));

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(detail));
                when(relatedKeywordRepository.findByName("Java")).thenReturn(Optional.of(mock(RelatedKeyword.class)));

                service.execute(request);

                verify(memberRepository, never()).existsByNickname(any());
                verify(member).updateNickname("테스터일");
                verify(detail).updateDescription("새 설명");
                verify(memberRelatedKeywordRepository).deleteAllByMember(member);
                verify(memberRelatedKeywordRepository).saveAll(any());
            }
        }

        @Nested
        @DisplayName("닉네임이 변경된 경우")
        class Context_with_new_nickname {

            @Test
            @DisplayName("중복 체크 후 닉네임을 변경한다")
            void it_checks_duplicate_and_updates_nickname() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(member.getNickname()).thenReturn("기존닉네임");

                MemberDetail detail = mock(MemberDetail.class);
                UpdateMyInfoRequest request = new UpdateMyInfoRequest("새닉네임", "설명", List.of());

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberRepository.existsByNickname("새닉네임")).thenReturn(false);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(detail));

                service.execute(request);

                verify(member).updateNickname("새닉네임");
            }
        }

        @Nested
        @DisplayName("변경하려는 닉네임이 이미 존재할 때")
        class Context_with_duplicate_nickname {

            @Test
            @DisplayName("DuplicateNicknameException을 던진다")
            void it_throws_duplicate_nickname_exception() {
                Member member = mock(Member.class);
                when(member.getNickname()).thenReturn("기존닉네임");

                UpdateMyInfoRequest request = new UpdateMyInfoRequest("중복닉네임", "설명", List.of());

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberRepository.existsByNickname("중복닉네임")).thenReturn(true);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(DuplicateNicknameException.class);
            }
        }

        @Nested
        @DisplayName("MemberDetail이 없을 때")
        class Context_with_detail_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(member.getNickname()).thenReturn("테스터일");

                UpdateMyInfoRequest request = new UpdateMyInfoRequest("테스터일", "설명", List.of());

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }
    }
}
