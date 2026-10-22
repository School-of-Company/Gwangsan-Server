package team.startup.gwangsan.adapter.web.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnswerFeedbackRequest(@NotBlank String answer) {
}
