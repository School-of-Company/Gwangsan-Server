package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.BalanceMismatch;

import java.util.List;

/** 회원 잔액과 광산 원장 합을 맞춰 보고 다른 회원을 돌려준다. 읽기만 한다. */
public interface ReconcileGwangsanUseCase {

    List<BalanceMismatch> execute();
}
