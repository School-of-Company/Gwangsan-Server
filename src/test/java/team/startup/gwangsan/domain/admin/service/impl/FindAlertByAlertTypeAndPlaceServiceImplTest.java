package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetAdminAlertResponse;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAlertByAlertTypeAndPlaceServiceImpl 단위 테스트")
class FindAlertByAlertTypeAndPlaceServiceImplTest {

    @InjectMocks private FindAlertByAlertTypeAndPlaceServiceImpl service;

    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private AdminAlertRepository adminAlertRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ReportImageRepository reportImageRepository;
    @Mock private PlaceRepository placeRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private TradeCancelRepository tradeCancelRepository;
    @Mock private TradeCancelImageRepository tradeCancelImageRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("ROLE_PLACE_ADMIN이고 알림이 없을 때")
        class Context_with_place_admin_and_no_alerts {

            @Test
            @DisplayName("빈 응답을 반환한다")
            void it_returns_empty_response() {
                Member admin = mock(Member.class);
                when(admin.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);

                Place place = mock(Place.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findPlaceByMemberId(any())).thenReturn(place);
                when(adminAlertRepository.findAdminAlertByPlacesAndAlertType(any(), any()))
                        .thenReturn(Collections.emptyList());
                when(memberDetailRepository.findPlaceNameMapByMemberIds(any())).thenReturn(Map.of());

                GetAdminAlertResponse response = service.execute(null, AlertType.SIGN_UP);

                assertThat(response.reports()).isEmpty();
                assertThat(response.signUps()).isEmpty();
                assertThat(response.tradeCancels()).isEmpty();
            }
        }

        @Nested
        @DisplayName("ROLE_HEAD_ADMIN이고 placeId가 null일 때")
        class Context_with_head_admin_and_no_place_id {

            @Test
            @DisplayName("산하 지점 전체의 빈 응답을 반환한다")
            void it_returns_empty_response_for_all_branches() {
                Member admin = mock(Member.class);
                when(admin.getRole()).thenReturn(MemberRole.ROLE_HEAD_ADMIN);

                Place place = mock(Place.class);
                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findPlaceByMemberId(any())).thenReturn(place);
                when(placeRepository.findByHead(any())).thenReturn(Collections.emptyList());
                when(adminAlertRepository.findAdminAlertByPlacesAndAlertType(any(), any()))
                        .thenReturn(Collections.emptyList());
                when(memberDetailRepository.findPlaceNameMapByMemberIds(any())).thenReturn(Map.of());

                GetAdminAlertResponse response = service.execute(null, AlertType.SIGN_UP);

                assertThat(response.reports()).isEmpty();
                assertThat(response.signUps()).isEmpty();
                assertThat(response.tradeCancels()).isEmpty();
            }
        }
    }
}
