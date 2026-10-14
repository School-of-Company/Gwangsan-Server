package team.startup.gwangsan.domain.admin.policy;

import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;

import java.util.List;
import java.util.Objects;

/**
 * 관리자가 관리할 수 있는 지점 범위. HEAD_ADMIN 은 같은 본부의 모든 지점, PLACE_ADMIN 은 자기 지점만.
 */
public record AdminPlaceScope(List<Integer> placeIds) {

    public static AdminPlaceScope ofHead(List<Integer> headPlaceIds) {
        return new AdminPlaceScope(List.copyOf(headPlaceIds));
    }

    public static AdminPlaceScope ofPlace(Integer placeId) {
        return new AdminPlaceScope(List.of(placeId));
    }

    public void validate(Integer targetPlaceId) {
        // 기존 ValidatePlaceUtil 은 대상 지점이 없으면 NPE(500)였다. 오류 의미를 바꾸지 않는다.
        Objects.requireNonNull(targetPlaceId, "targetPlaceId");
        if (!placeIds.contains(targetPlaceId)) {
            throw new AdminPlaceMismatchException();
        }
    }
}
