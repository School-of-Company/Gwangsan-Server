package team.startup.gwangsan.adapter.persistence.trade;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_trade_cancel_image")
public class TradeCancelImageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_cancel_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_cancel_id", nullable = false)
    private TradeCancelJpaEntity tradeCancel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageJpaEntity image;

    @Builder
    public TradeCancelImageJpaEntity(TradeCancelJpaEntity tradeCancel, ImageJpaEntity image) {
        this.tradeCancel = tradeCancel;
        this.image = image;
    }
}
