package team.startup.gwangsan.domain.admin.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.exception.PlaceMismatchException;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ValidatePlaceUtil {

    private final PlaceRepository placeRepository;

    public void validateSamePlace(MemberJpaEntity admin, MemberDetailJpaEntity adminDetail, MemberDetailJpaEntity targetDetail) {
        List<Place> adminPlaces = getAdminPlaces(admin, adminDetail);
        validatePlaceAuthority(adminPlaces, targetDetail.getPlace());
    }

    private List<Place> getAdminPlaces(MemberJpaEntity admin, MemberDetailJpaEntity detail) {
        return admin.getRole() == MemberRole.ROLE_HEAD_ADMIN
                ? placeRepository.findByHead(detail.getPlace().getHead())
                : List.of(detail.getPlace());
    }

    private void validatePlaceAuthority(List<Place> places, Place targetPlace) {
        boolean hasAuthority = places.stream()
                .anyMatch(p -> p.getId().equals(targetPlace.getId()));
        if (!hasAuthority) throw new PlaceMismatchException();
    }
}
