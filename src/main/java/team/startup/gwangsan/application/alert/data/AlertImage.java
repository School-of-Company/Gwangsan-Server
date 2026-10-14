package team.startup.gwangsan.application.alert.data;

/** imageId 는 기존 응답과 같이 각 업무의 이미지 연결 행 ID 다. */
public record AlertImage(
        Long imageId,
        String imageUrl
) {
}
