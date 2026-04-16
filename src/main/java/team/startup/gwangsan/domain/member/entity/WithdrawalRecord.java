package team.startup.gwangsan.domain.member.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_withdrawal_record")
@EntityListeners(AuditingEntityListener.class)
public class WithdrawalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_record_id")
    private Long id;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "gwangsan", nullable = false)
    private Integer gwangsan;

    @Column(name = "banned", nullable = false)
    private boolean banned;

    @CreatedDate
    @Column(name = "withdrawal_at", nullable = false)
    private LocalDateTime withdrawalAt;

    @Builder
    public WithdrawalRecord(String phoneNumber, Integer gwangsan, boolean banned) {
        this.phoneNumber = phoneNumber;
        this.gwangsan = gwangsan;
        this.banned = banned;
    }
}
