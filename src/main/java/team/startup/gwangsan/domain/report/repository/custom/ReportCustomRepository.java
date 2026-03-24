package team.startup.gwangsan.domain.report.repository.custom;

import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.report.entity.Report;

import java.util.List;

public interface ReportCustomRepository {
    List<Report> findByPlaces(List<Place> places);
}
