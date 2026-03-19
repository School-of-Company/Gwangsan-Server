package team.startup.gwangsan.global.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;

@Aspect
@Component
@RequiredArgsConstructor
public class BlockAspect {

    private final MemberUtil memberUtil;
    private final BlockValidator blockValidator;

    @Before("@annotation(checkBlocked)")
    public void check(JoinPoint joinPoint, CheckBlocked checkBlocked) {
        String paramName = checkBlocked.param();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        Long targetMemberId = null;
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                targetMemberId = (Long) args[i];
                break;
            }
        }

        if (targetMemberId == null) {
            throw new IllegalArgumentException("@CheckBlocked param 이름이 잘못되었습니다: " + paramName);
        }

        Member currentMember = memberUtil.getCurrentMember();
        blockValidator.validate(currentMember.getId(), targetMemberId);
    }
}
