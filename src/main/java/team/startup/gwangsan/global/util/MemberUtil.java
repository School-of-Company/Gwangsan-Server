package team.startup.gwangsan.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.security.auth.MemberDetails;
import team.startup.gwangsan.adapter.security.auth.exception.InvalidMemberPrincipalException;

@Component
@RequiredArgsConstructor
public class MemberUtil {

    private final MemberJpaRepository memberRepository;

    public MemberJpaEntity getCurrentMember() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof MemberDetails memberDetails) {
            String phoneNumber = memberDetails.getUsername();

            return memberRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(NotFoundMemberException::new);
        }

        throw new InvalidMemberPrincipalException();
    }
}
