package team.startup.gwangsan.domain.admin.repository.custom;

import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;

public interface AdminAlertCustomRepository {
    List<AdminAlert> findAdminAlertByPlaceAndAlertType(Place place, AlertType alertType);
}
