package team.startup.gwangsan.domain.trade.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.image.entity.Image;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_trade_cancel_image")
public class TradeCancelImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_cancel_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_cancel_id", nullable = false)
    private TradeCancel tradeCancel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Builder
    public TradeCancelImage(TradeCancel tradeCancel, Image image) {
        this.tradeCancel = tradeCancel;
        this.image = image;
    }
}
