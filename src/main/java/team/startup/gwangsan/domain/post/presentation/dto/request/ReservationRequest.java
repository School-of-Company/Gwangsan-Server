package team.startup.gwangsan.domain.post.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull Long roomId,
        @NotNull LocalDateTime scheduledAt,
        @NotNull @Size(max = 100) String location
) {
}
