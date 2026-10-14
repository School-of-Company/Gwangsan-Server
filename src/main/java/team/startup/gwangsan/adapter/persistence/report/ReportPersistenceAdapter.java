package team.startup.gwangsan.adapter.persistence.report;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.data.ReportDetail;
import team.startup.gwangsan.application.report.data.ReportImageLink;
import team.startup.gwangsan.application.report.port.out.LoadReportPort;
import team.startup.gwangsan.application.report.port.out.SaveReportPort;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.report.model.Report;
import team.startup.gwangsan.domain.report.model.ReportType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 신고 저장. 회원·게시글·이미지 association 은 기존 FK 매핑 유지를 위한 저장 기술 예외로,
 * 이미 존재가 확인된 ID 의 reference 로만 채운다.
 */
@Component
@RequiredArgsConstructor
public class ReportPersistenceAdapter implements LoadReportPort, SaveReportPort {

    private final ReportJpaRepository reportJpaRepository;
    private final ReportImageJpaRepository reportImageJpaRepository;
    private final EntityManager entityManager;

    @Override
    public boolean existsMemberReport(Long reporterId, Long reportedId, ReportType reportType) {
        return reportJpaRepository
                .findByReporterIdAndReportedIdAndReportTypeAndProductIsNull(reporterId, reportedId, reportType)
                .isPresent();
    }

    @Override
    public boolean existsProductReport(Long reporterId, Long productId, ReportType reportType) {
        return reportJpaRepository.findByReporterIdAndProductIdAndReportType(reporterId, productId, reportType)
                .isPresent();
    }

    @Override
    public Long save(Report report) {
        ReportJpaEntity entity = ReportJpaEntity.builder()
                .reportType(report.reportType())
                .content(report.content())
                .reporter(entityManager.getReference(MemberJpaEntity.class, report.reporterId()))
                .reported(entityManager.getReference(MemberJpaEntity.class, report.reportedId()))
                .product(report.productId() == null ? null : entityManager.getReference(Product.class, report.productId()))
                .build();
        return reportJpaRepository.save(entity).getId();
    }

    @Override
    public void saveImages(Long reportId, List<Long> imageIds) {
        ReportJpaEntity report = entityManager.getReference(ReportJpaEntity.class, reportId);
        reportImageJpaRepository.saveAll(imageIds.stream()
                .map(imageId -> new ReportImageJpaEntity(entityManager.getReference(Image.class, imageId), report))
                .toList());
    }

    @Override
    public Optional<ReportAlertSource> findAlertSource(Long reportId) {
        return reportJpaRepository.findById(reportId)
                .map(report -> new ReportAlertSource(report.getReporter().getId(), report.getReported().getNickname()));
    }

    @Override
    public Map<Long, List<ReportImageLink>> findImageLinks(Collection<Long> reportIds) {
        return reportImageJpaRepository.findAllByReportIdIn(reportIds).stream()
                .collect(Collectors.groupingBy(
                        link -> link.getReport().getId(),
                        Collectors.mapping(link -> new ReportImageLink(link.getId(), link.getImage().getImageUrl()),
                                Collectors.toList())));
    }

    @Override
    public void deleteImageLink(Long reportId, Long imageId) {
        reportImageJpaRepository.deleteByReportIdAndImageId(reportId, imageId);
    }

    @Override
    public Optional<ReportType> findReportType(Long reportId) {
        return reportJpaRepository.findById(reportId).map(ReportJpaEntity::getReportType);
    }

    // 기존 관리자 조회와 같은 쿼리: findAllByIdIn(게시글 EntityGraph) 1회 → 신고가 있으면 이미지 fetch join 1회,
    // 피신고자 닉네임은 지연 로딩(같은 영속성 컨텍스트에서 회원당 1회).
    @Override
    public List<ReportDetail> findDetails(Collection<Long> reportIds) {
        List<ReportJpaEntity> reports = reportJpaRepository.findAllByIdIn(List.copyOf(reportIds));
        if (reports.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ReportDetail.ReportImageRef>> images = reportImageJpaRepository.findAllByReportIdIn(reportIds).stream()
                .collect(Collectors.groupingBy(
                        link -> link.getReport().getId(),
                        Collectors.mapping(link -> new ReportDetail.ReportImageRef(link.getImage().getId(), link.getImage().getImageUrl()),
                                Collectors.toList())));
        return reports.stream()
                .map(report -> {
                    MemberJpaEntity reported = report.getReported();
                    Product product = report.getProduct();
                    return new ReportDetail(
                            report.getId(),
                            report.getReportType(),
                            report.getContent(),
                            reported != null ? reported.getId() : null,
                            reported != null ? reported.getNickname() : null,
                            product != null ? product.getId() : null,
                            product != null ? product.getTitle() : null,
                            images.getOrDefault(report.getId(), List.of()));
                })
                .toList();
    }

    // 회원 association 은 bulk 쿼리마다 새 reference 로 넘긴다(앞 쿼리의 clearAutomatically 이후에도 안전).
    @Override
    public void reassignMember(Long memberId, Long dummyMemberId) {
        reportJpaRepository.reassignReporter(memberReference(memberId), memberReference(dummyMemberId));
        reportJpaRepository.reassignReported(memberReference(memberId), memberReference(dummyMemberId));
    }

    private MemberJpaEntity memberReference(Long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }
}
