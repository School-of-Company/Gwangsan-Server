package team.startup.gwangsan.domain.admin.repository.custom;

import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;
import java.util.Optional;

public interface AdminAlertCustomRepository {
    List<AdminAlert> findAdminAlertByPlacesAndAlertType(List<Place> places, AlertType alertType);

    Optional<AdminAlert> findByIdWithMember(Long id);
}
