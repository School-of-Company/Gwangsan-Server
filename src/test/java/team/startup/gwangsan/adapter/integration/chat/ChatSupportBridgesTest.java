package team.startup.gwangsan.adapter.integration.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEvent;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("chat 공개 Port 연결 adapter (image·notification·member) 단위 테스트")
class ChatSupportBridgesTest {

    @Nested
    @DisplayName("ChatImageBridge 는")
    class Describe_image {

        @Test
        @DisplayName("it_모두 있으면 ID→URL 을, 하나라도 없으면 기존 ImageNotFoundException 을 낸다")
        void it_requires_all_images() {
            ImageQuery images = mock(ImageQuery.class);
            when(images.getAllByIds(List.of(11L, 11L)))
                    .thenReturn(List.of(new ImageSummary(11L, "u11"), new ImageSummary(11L, "u11")));
            when(images.getAllByIds(List.of(11L, 12L))).thenThrow(new ImageNotFoundException());
            ChatImageBridge bridge = new ChatImageBridge(images);

            assertThat(bridge.requireImageUrls(List.of(11L, 11L))).containsExactlyEntriesOf(Map.of(11L, "u11"));
            assertThatThrownBy(() -> bridge.requireImageUrls(List.of(11L, 12L))).isInstanceOf(ImageNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("ChatPushBridge 는")
    class Describe_push {

        @Test
        @DisplayName("it_수신자 회원 ID 로 CHATTING 푸시 요청 이벤트를 낸다 (토큰 조회는 커밋 후 notification)")
        void it_publishes_push_request_by_member_id() {
            ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

            new ChatPushBridge(events).notifyNewMessage(2L, 10L);

            verify(events).publishEvent(new PushRequestedEvent(List.of(2L), NotificationType.CHATTING, 10L));
            verifyNoMoreInteractions(events);
        }
    }

    @Nested
    @DisplayName("ChatMemberBridge 는")
    class Describe_member {

        @Test
        @DisplayName("it_회원 공개 query 로 존재 여부를 판단한다")
        void it_checks_existence_via_member_query() {
            MemberAccountQuery members = mock(MemberAccountQuery.class);
            when(members.findById(1L)).thenReturn(Optional.of(mock(MemberAccount.class)));
            when(members.findById(2L)).thenReturn(Optional.empty());
            ChatMemberBridge bridge = new ChatMemberBridge(members);

            assertThat(bridge.exists(1L)).isTrue();
            assertThat(bridge.exists(2L)).isFalse();
        }
    }
}
