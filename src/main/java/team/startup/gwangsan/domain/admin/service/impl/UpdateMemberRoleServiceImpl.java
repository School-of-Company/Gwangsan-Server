package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.service.UpdateMemberRoleService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class UpdateMemberRoleServiceImpl implements UpdateMemberRoleService {

    private final MemberJpaRepository memberRepository;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final MemberUtil memberUtil;
    private final ValidatePlaceUtil validatePlaceUtil;
    private final PlaceRepository placeRepository;

    @Override
    @Transactional
    public void execute(Long memberId, MemberRole role, Integer placeId) {
        MemberJpaEntity admin = memberUtil.getCurrentMember();
        MemberDetailJpaEntity adminDetail = memberDetailRepository.findById(admin.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        MemberJpaEntity targetMember = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);
        MemberDetailJpaEntity targetMemberDetail = memberDetailRepository.findById(targetMember.getId())
                        .orElseThrow(NotFoundMemberDetailException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, targetMemberDetail);

        if (placeId != null) {
            Place place = placeRepository.findById(placeId)
                    .orElseThrow(PlaceNotFoundException::new);

            targetMemberDetail.updatePlace(place);
        }

        targetMember.updateMemberRole(role);
    }
}
