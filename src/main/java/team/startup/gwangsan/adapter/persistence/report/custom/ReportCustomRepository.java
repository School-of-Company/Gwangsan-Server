package team.startup.gwangsan.adapter.persistence.report.custom;

import team.startup.gwangsan.adapter.persistence.report.ReportJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;

import java.util.List;

public interface ReportCustomRepository {
    List<ReportJpaEntity> findByPlaces(List<PlaceJpaEntity> places);
}
