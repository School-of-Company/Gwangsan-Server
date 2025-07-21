package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomsResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.FindRoomsByCurrentUserService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindRoomsByCurrentUserServiceImpl implements FindRoomsByCurrentUserService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<GetRoomsResponse> execute() {
        return chatRoomRepository.findRoomsByMemberId(memberUtil.getCurrentMember().getId());
    }
}
