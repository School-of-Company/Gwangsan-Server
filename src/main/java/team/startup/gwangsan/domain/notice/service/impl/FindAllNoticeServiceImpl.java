package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindAllNoticeResponse;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.service.FindAllNoticeService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindAllNoticeServiceImpl implements FindAllNoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;

    @Override
    public List<FindAllNoticeResponse> execute(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return noticeRepository.findAll(pageable).getContent().stream()
                .map(notice -> {
                    List<NoticeImage> noticeImages = noticeImageRepository.findAllByNotice(notice);

                    List<GetImageResponse> imageResponses = noticeImages.stream()
                            .map(ni -> new GetImageResponse(
                                    ni.getImage().getId(),
                                    ni.getImage().getImageUrl()
                            ))
                            .collect(Collectors.toList());

                    return new FindAllNoticeResponse(
                            notice.getId(),
                            notice.getTitle(),
                            notice.getContent(),
                            notice.getPlace().getName(),
                            notice.getCreatedAt().format(formatter),
                            notice.getMember().getRole().name(),
                            imageResponses
                    );
                })
                .collect(Collectors.toList());
    }
}
