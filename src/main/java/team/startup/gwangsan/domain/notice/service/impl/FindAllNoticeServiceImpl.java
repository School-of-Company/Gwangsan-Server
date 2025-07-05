package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.service.FindAllNoticeService;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindAllNoticeResponse;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindAllNoticeServiceImpl implements FindAllNoticeService {

    private final NoticeRepository noticeRepository;

    @Override
    public List<FindAllNoticeResponse> execute(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return noticeRepository.findAll(pageable).getContent().stream()
                .map(notice -> new FindAllNoticeResponse(
                        notice.getId(),
                        notice.getTitle(),
                        notice.getContent(),
                        notice.getPlace().getName(),
                        notice.getCreatedAt().format(formatter),
                        notice.getMember().getRole().name(),
                        notice.getNoticeImages().stream()
                                .map(noticeImage -> new GetImageResponse(
                                        noticeImage.getImage().getId(),
                                        noticeImage.getImage().getImageUrl()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
