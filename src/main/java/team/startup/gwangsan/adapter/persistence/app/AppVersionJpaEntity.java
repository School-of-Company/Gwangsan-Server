package team.startup.gwangsan.adapter.persistence.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_app_version")
public class AppVersionJpaEntity {

    @Id
    @Column(length = 7)
    private String platform;

    @Column(name = "latest_version", nullable = false, length = 32)
    private String latestVersion;

    @Column(name = "minimum_version", nullable = false, length = 32)
    private String minimumVersion;
}
