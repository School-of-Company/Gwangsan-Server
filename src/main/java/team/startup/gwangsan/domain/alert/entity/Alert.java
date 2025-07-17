package team.startup.gwangsan.domain.alert.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_alert")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long id;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "send_member_id")
    private Member sendMember;

    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Builder
    public Alert(Long sourceId, Member member, Member sendMember, AlertType alertType, String title, String content) {
        this.sourceId = sourceId;
        this.member = member;
        this.sendMember = sendMember;
        this.alertType = alertType;
        this.title = title;
        this.content = content;
    }


}
