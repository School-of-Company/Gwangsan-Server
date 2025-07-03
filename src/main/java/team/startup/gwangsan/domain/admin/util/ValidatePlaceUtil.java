package team.startup.gwangsan.domain.admin.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.repository.custom.MemberDetailCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.exception.PlaceMismatchException;

@Component
@RequiredArgsConstructor
public class ValidatePlaceUtil {

    private final MemberDetailCustomRepository memberDetailCustomRepository;

    public void validateSamePlace(Member admin, Member target) {
        if (!admin.getRole().equals(MemberRole.ROLE_HEAD_ADMIN)) {
            Place adminPlace = memberDetailCustomRepository.findPlaceByMemberId(admin.getId());
            Place targetPlace = memberDetailCustomRepository.findPlaceByMemberId(target.getId());

            if (!targetPlace.isSamePlace(adminPlace)) {
                throw new PlaceMismatchException();
            }
        }
    }
}
