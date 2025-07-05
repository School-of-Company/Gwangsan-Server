package team.startup.gwangsan.domain.image.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_image")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Builder
    public Image(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
