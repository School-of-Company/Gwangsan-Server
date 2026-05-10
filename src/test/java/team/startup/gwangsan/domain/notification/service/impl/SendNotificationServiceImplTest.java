package team.startup.gwangsan.domain.notification.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.notification.NotificationPort;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.List;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendNotificationServiceImpl 단위 테스트")
class SendNotificationServiceImplTest {

    @InjectMocks
    private SendNotificationServiceImpl service;

    @Mock
    private NotificationPort notificationPort;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("NOTICE 타입일 때")
        class Context_with_notice_type {

            @Test
            @DisplayName("공지 body로 알림을 전송한다")
            void it_sends_notification_with_notice_body() {
                List<DeviceToken> tokens = List.of(mock(DeviceToken.class));

                service.execute(tokens, NotificationType.NOTICE, 1L);

                verify(notificationPort).sendNotification(
                        eq(tokens),
                        eq("시민화폐 광산"),
                        eq("새로운 공지가 등록되었습니다."),
                        eq(NotificationType.NOTICE),
                        eq(1L)
                );
            }
        }

        @Nested
        @DisplayName("CHATTING 타입일 때")
        class Context_with_chatting_type {

            @Test
            @DisplayName("채팅 body로 알림을 전송한다")
            void it_sends_notification_with_chatting_body() {
                List<DeviceToken> tokens = List.of(mock(DeviceToken.class));

                service.execute(tokens, NotificationType.CHATTING, 1L);

                verify(notificationPort).sendNotification(
                        eq(tokens),
                        eq("시민화폐 광산"),
                        eq("새로운 메세지가 도착했습니다."),
                        eq(NotificationType.CHATTING),
                        eq(1L)
                );
            }
        }

        @Nested
        @DisplayName("TRADE_COMPLETE 타입일 때")
        class Context_with_trade_complete_type {

            @Test
            @DisplayName("거래 완료 body로 알림을 전송한다")
            void it_sends_notification_with_trade_complete_body() {
                List<DeviceToken> tokens = List.of(mock(DeviceToken.class));

                service.execute(tokens, NotificationType.TRADE_COMPLETE, 1L);

                verify(notificationPort).sendNotification(
                        eq(tokens),
                        eq("시민화폐 광산"),
                        eq("거래가 승인되었습니다."),
                        eq(NotificationType.TRADE_COMPLETE),
                        eq(1L)
                );
            }
        }

        @Nested
        @DisplayName("RECOMMENDATION 타입일 때")
        class Context_with_recommendation_type {

            @Test
            @DisplayName("추천인 body로 알림을 전송한다")
            void it_sends_notification_with_recommendation_body() {
                List<DeviceToken> tokens = List.of(mock(DeviceToken.class));

                service.execute(tokens, NotificationType.RECOMMENDATION, 1L);

                verify(notificationPort).sendNotification(
                        eq(tokens),
                        eq("시민화폐 광산"),
                        eq("추천인이 등록되었습니다."),
                        eq(NotificationType.RECOMMENDATION),
                        eq(1L)
                );
            }
        }
    }
}