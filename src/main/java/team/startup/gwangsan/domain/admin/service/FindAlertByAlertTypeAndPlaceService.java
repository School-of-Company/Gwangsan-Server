package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetAdminAlertResponse;

public interface FindAlertByAlertTypeAndPlaceService {
    GetAdminAlertResponse execute(Integer placeId, AlertType type);
}
