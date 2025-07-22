package team.startup.gwangsan.domain.alert.service;

import team.startup.gwangsan.domain.alert.presentation.dto.response.GetAlertResponse;

import java.util.List;

public interface FindAlertByCurrentService {
    List<GetAlertResponse> execute();
}
