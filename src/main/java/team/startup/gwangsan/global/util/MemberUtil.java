package team.startup.gwangsan.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.auth.MemberDetails;
import team.startup.gwangsan.global.security.exception.InvalidMemberPrincipalException;

@Component
@RequiredArgsConstructor
public class MemberUtil {

    private final MemberRepository memberRepository;

    public Member getCurrentMember() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof MemberDetails memberDetails) {
            String phoneNumber = memberDetails.getUsername();

            return memberRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(NotFoundMemberException::new);
        }

        throw new InvalidMemberPrincipalException();
    }
}
