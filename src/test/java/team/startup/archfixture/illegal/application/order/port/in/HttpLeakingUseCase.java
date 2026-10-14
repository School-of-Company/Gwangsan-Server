package team.startup.archfixture.illegal.application.order.port.in;

import jakarta.servlet.http.HttpServletRequest;

public interface HttpLeakingUseCase {
    void execute(HttpServletRequest request);
}
