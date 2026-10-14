package team.startup.gwangsan.adapter.security.auth;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.security.exception.InvalidMemberPrincipalException;

/**
 * Controller 의 {@link ActorContext} 파라미터를 인증 principal 로만 채운다.
 * principal 의 Member 는 이번 요청에서 JwtAuthenticationFilter 가 DB 에서 읽은 값이다.
 */
@Component
public class ActorContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ActorContext.class.equals(parameter.getParameterType());
    }

    @Override
    public ActorContext resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MemberDetails memberDetails) {
            Member member = memberDetails.getMember();
            return new ActorContext(member.getId(), ActorContext.Role.valueOf(member.getRole().name()));
        }
        throw new InvalidMemberPrincipalException();
    }
}
