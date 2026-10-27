package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.BalanceMismatch;
import team.startup.gwangsan.application.member.port.in.ReconcileGwangsanUseCase;
import team.startup.gwangsan.application.member.port.out.GwangsanBalancePort;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconcileGwangsanService implements ReconcileGwangsanUseCase {

    private final GwangsanBalancePort gwangsanBalancePort;

    @Override
    @Transactional(readOnly = true)
    public List<BalanceMismatch> execute() {
        return gwangsanBalancePort.findMismatches();
    }
}
