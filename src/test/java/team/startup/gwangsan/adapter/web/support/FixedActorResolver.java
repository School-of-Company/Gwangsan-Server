package team.startup.gwangsan.adapter.web.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import team.startup.gwangsan.domain.common.ActorContext;

/** standalone MockMvc 용: 인증 principal 대신 고정 실행자를 넣는다. */
public record FixedActorResolver(ActorContext actor) implements HandlerMethodArgumentResolver {

    public static FixedActorResolver user(long memberId) {
        return new FixedActorResolver(new ActorContext(memberId, ActorContext.Role.ROLE_USER));
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ActorContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return actor;
    }
}
