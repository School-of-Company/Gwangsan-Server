package team.startup.gwangsan.adapter.integration.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.notification.data.RegisterDeviceTokenCommand;
import team.startup.gwangsan.application.notification.port.in.RegisterDeviceTokenUseCase;
import team.startup.gwangsan.domain.notification.model.OsType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("DeviceTokenAdapter")
class DeviceTokenAdapterTest {

    private final RegisterDeviceTokenUseCase useCase = mock(RegisterDeviceTokenUseCase.class);
    private final DeviceTokenAdapter adapter = new DeviceTokenAdapter(useCase);

    @Test
    @DisplayName("로그인 기기 정보를 notification 기기 등록 계약으로 그대로 넘긴다")
    void it_registers_through_notification_port() {
        adapter.register("device", 7L, "ExponentPushToken[x]", "IOS");

        verify(useCase).register(new RegisterDeviceTokenCommand("device", 7L, "ExponentPushToken[x]", OsType.IOS));
    }

    @Test
    @DisplayName("osType 이 없으면 기존처럼 null 로 저장한다")
    void it_keeps_null_os_type() {
        adapter.register("device", 7L, "tok", null);

        verify(useCase).register(new RegisterDeviceTokenCommand("device", 7L, "tok", null));
    }
}
