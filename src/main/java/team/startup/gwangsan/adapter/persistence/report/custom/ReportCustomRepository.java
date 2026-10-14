package team.startup.gwangsan.adapter.persistence.report.custom;

import team.startup.gwangsan.adapter.persistence.report.ReportJpaEntity;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;

public interface ReportCustomRepository {
    List<ReportJpaEntity> findByPlaces(List<Place> places);
}
