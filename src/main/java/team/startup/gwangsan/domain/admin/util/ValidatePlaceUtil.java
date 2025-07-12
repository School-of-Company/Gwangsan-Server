package team.startup.gwangsan.domain.admin.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.exception.PlaceMismatchException;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ValidatePlaceUtil {

    private final PlaceRepository placeRepository;

    public void validateSamePlace(Member admin, MemberDetail adminDetail, MemberDetail targetDetail) {
        List<Place> adminPlaces = getAdminPlaces(admin, adminDetail);
        validatePlaceAuthority(adminPlaces, targetDetail.getPlace());
    }

    private List<Place> getAdminPlaces(Member admin, MemberDetail detail) {
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
