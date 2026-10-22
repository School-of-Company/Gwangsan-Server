package team.startup.gwangsan.adapter.web.member.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateMyPlaceRequest(@NotNull Integer placeId) {
}
