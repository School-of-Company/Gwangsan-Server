package team.startup.gwangsan.adapter.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GwangsanLedgerJpaRepository extends JpaRepository<GwangsanLedgerJpaEntity, Long> {

    /** [member_id, gwangsan, ledger_sum]. 탈퇴 회원은 상세가 없어 자연히 빠진다. */
    @Query(value = """
            SELECT d.member_id, d.gwangsan, COALESCE(SUM(l.amount), 0) AS ledger_sum
            FROM tbl_member_detail d LEFT JOIN tbl_gwangsan_ledger l ON l.member_id = d.member_id
            GROUP BY d.member_id, d.gwangsan
            HAVING d.gwangsan <> ledger_sum
            """, nativeQuery = true)
    List<Object[]> findMismatches();
}
