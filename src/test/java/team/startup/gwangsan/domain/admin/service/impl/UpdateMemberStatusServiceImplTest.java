package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.repository.WithdrawalRecordRepository;
import team.startup.gwangsan.domain.member.service.MemberDeletionService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import team.startup.gwangsan.domain.member.entity.WithdrawalRecord;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMemberStatusServiceImpl 단위 테스트")
class UpdateMemberStatusServiceImplTest {

    @InjectMocks
    private UpdateMemberStatusServiceImpl service;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private ValidatePlaceUtil validatePlaceUtil;

    @Mock
    private WithdrawalRecordRepository withdrawalRecordRepository;

    @Mock
    private MemberDeletionService memberDeletionService;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("대상 회원의 상태를 변경한다")
            void it_updates_member_status() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);

                Member target = mock(Member.class);
                when(target.getId()).thenReturn(2L);
                MemberDetail targetDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(target));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(targetDetail));

                service.execute(2L, MemberStatus.SUSPENDED);

                verify(target).updateMemberStatus(MemberStatus.SUSPENDED);
                verify(validatePlaceUtil).validateSamePlace(admin, adminDetail, targetDetail);
            }
        }

        @Nested
        @DisplayName("상태가 WITHDRAWN일 때")
        class Context_with_withdrawn_status {

            @Test
            @DisplayName("탈퇴 기록을 저장하고 회원을 삭제한다")
            void it_saves_withdrawal_record_and_deletes_member() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);

                Member target = mock(Member.class);
                when(target.getId()).thenReturn(2L);
                when(target.getPhoneNumber()).thenReturn("01012345678");
                MemberDetail targetDetail = mock(MemberDetail.class);
                when(targetDetail.getGwangsan()).thenReturn(5000);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(target));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(targetDetail));

                service.execute(2L, MemberStatus.WITHDRAWN);

                verify(withdrawalRecordRepository).save(argThat((WithdrawalRecord record) ->
                        "01012345678".equals(record.getPhoneNumber())
                                && record.getGwangsan() == 5000
                                && record.isBanned()
                ));
                verify(memberDeletionService).delete(target);
            }
        }

        @Nested
        @DisplayName("대상 회원이 없을 때")
        class Context_with_target_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberRepository.findById(2L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(2L, MemberStatus.SUSPENDED))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }
    }
}
