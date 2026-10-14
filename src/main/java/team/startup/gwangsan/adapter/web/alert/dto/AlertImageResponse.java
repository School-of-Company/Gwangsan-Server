package team.startup.gwangsan.adapter.web.alert.dto;

// 기존 GetImageResponse 와 같은 JSON 형태(imageId, imageUrl).
public record AlertImageResponse(
        Long imageId,
        String imageUrl
) {
}
