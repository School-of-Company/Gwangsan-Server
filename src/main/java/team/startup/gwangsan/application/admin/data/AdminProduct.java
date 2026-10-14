package team.startup.gwangsan.application.admin.data;

import java.util.List;

/** 관리 화면의 상품 요약. type/mode 는 post 의 Type/Mode 이름. */
public record AdminProduct(
        Long id,
        String title,
        String content,
        Integer gwangsan,
        String type,
        String mode,
        Owner member,
        List<AdminImage> images,
        boolean completed,
        boolean reserved
) {
    public record Owner(Long memberId, String nickname, String placeName, Integer light) {
    }
}
