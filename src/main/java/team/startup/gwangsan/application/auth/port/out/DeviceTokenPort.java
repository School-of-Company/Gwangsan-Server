package team.startup.gwangsan.application.auth.port.out;

/** 푸시 기기 토큰 등록. notification 업무 소유 데이터다. */
public interface DeviceTokenPort {

    void register(String deviceId, Long memberId, String deviceToken, String osType);
}
