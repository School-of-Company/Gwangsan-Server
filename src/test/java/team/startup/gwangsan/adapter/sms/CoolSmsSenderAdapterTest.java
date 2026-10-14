package team.startup.gwangsan.adapter.sms;

import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.startup.gwangsan.domain.sms.exception.SmsSendFailedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CoolSmsSenderAdapter")
class CoolSmsSenderAdapterTest {

    private final DefaultMessageService messageService = mock(DefaultMessageService.class);
    private final CoolSmsSenderAdapter adapter =
            new CoolSmsSenderAdapter(messageService, new SmsProperties("key", "secret", "01000000000"));

    @Nested
    @DisplayName("send() 는")
    class Describe_send {

        @Test
        @DisplayName("설정의 발신 번호로 한 건 보낸다")
        void it_sends_from_configured_number() throws Exception {
            adapter.send("01012345678", "본문");

            ArgumentCaptor<SingleMessageSendingRequest> request = ArgumentCaptor.forClass(SingleMessageSendingRequest.class);
            verify(messageService).sendOne(request.capture());
            assertThat(request.getValue().getMessage().getFrom()).isEqualTo("01000000000");
            assertThat(request.getValue().getMessage().getTo()).isEqualTo("01012345678");
            assertThat(request.getValue().getMessage().getText()).isEqualTo("본문");
        }

        @Test
        @DisplayName("제공자 실패를 SmsSendFailedException 으로 바꾼다")
        void it_converts_provider_failure() throws Exception {
            doThrow(new RuntimeException("provider failed")).when(messageService).sendOne(any());

            assertThatThrownBy(() -> adapter.send("01012345678", "본문")).isInstanceOf(SmsSendFailedException.class);
        }
    }

    @Nested
    @DisplayName("sendAsync() 는")
    class Describe_send_async {

        @Test
        @DisplayName("제공자 실패를 로그만 남기고 삼킨다")
        void it_swallows_provider_failure() throws Exception {
            doThrow(new RuntimeException("provider failed")).when(messageService).sendOne(any());

            assertThatCode(() -> adapter.sendAsync("01012345678", "본문")).doesNotThrowAnyException();
            verify(messageService).sendOne(any());
        }
    }
}
