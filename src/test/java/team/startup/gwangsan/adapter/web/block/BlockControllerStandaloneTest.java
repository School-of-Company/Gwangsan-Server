package team.startup.gwangsan.adapter.web.block;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.adapter.web.FixedActorResolver;
import team.startup.gwangsan.application.block.data.BlockedMember;
import team.startup.gwangsan.application.block.data.MemberBlockCommand;
import team.startup.gwangsan.application.block.port.in.BlockListQuery;
import team.startup.gwangsan.application.block.port.in.BlockMemberUseCase;
import team.startup.gwangsan.application.block.port.in.UnblockMemberUseCase;
import team.startup.gwangsan.domain.block.exception.AlreadyBlockedException;
import team.startup.gwangsan.domain.block.exception.NotFoundBlockException;
import team.startup.gwangsan.domain.block.exception.SelfBlockNotAllowedException;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("차단 API 독립 HTTP 계약")
class BlockControllerStandaloneTest {

    private MockMvc mvc;
    private BlockMemberUseCase blockMemberUseCase;
    private UnblockMemberUseCase unblockMemberUseCase;
    private BlockListQuery blockListQuery;

    @BeforeEach
    void setUp() {
        blockMemberUseCase = mock(BlockMemberUseCase.class);
        unblockMemberUseCase = mock(UnblockMemberUseCase.class);
        blockListQuery = mock(BlockListQuery.class);
        mvc = MockMvcBuilders.standaloneSetup(new BlockController(blockMemberUseCase, unblockMemberUseCase, blockListQuery))
                .setCustomArgumentResolvers(FixedActorResolver.user(1L))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/block/{targetMemberId} 는")
    class Describe_block {

        @Test
        @DisplayName("it_인증 실행자로 차단하고 201 빈 본문을 반환한다")
        void it_returns_created() throws Exception {
            mvc.perform(post("/api/block/2"))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(blockMemberUseCase).execute(new MemberBlockCommand(1L, 2L));
        }

        @Test
        @DisplayName("it_자기 차단은 403, 중복 차단은 409 로 응답한다")
        void it_maps_business_errors() throws Exception {
            doThrow(new SelfBlockNotAllowedException()).when(blockMemberUseCase).execute(new MemberBlockCommand(1L, 1L));
            doThrow(new AlreadyBlockedException()).when(blockMemberUseCase).execute(new MemberBlockCommand(1L, 2L));

            mvc.perform(post("/api/block/1")).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
            mvc.perform(post("/api/block/2")).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("DELETE /api/block/{targetMemberId} 는")
    class Describe_unblock {

        @Test
        @DisplayName("it_204 를 반환하고 차단 내역이 없으면 404 로 응답한다")
        void it_returns_no_content_or_not_found() throws Exception {
            mvc.perform(delete("/api/block/2")).andExpect(status().isNoContent());
            verify(unblockMemberUseCase).execute(new MemberBlockCommand(1L, 2L));

            doThrow(new NotFoundBlockException()).when(unblockMemberUseCase).execute(any());
            mvc.perform(delete("/api/block/3")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/block 는")
    class Describe_list {

        @Test
        @DisplayName("it_memberId·nickname 배열을 200 으로 반환한다")
        void it_returns_blocked_members() throws Exception {
            when(blockListQuery.query(1L)).thenReturn(List.of(new BlockedMember(10L, "차단유저A")));

            mvc.perform(get("/api/block"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].memberId").value(10))
                    .andExpect(jsonPath("$[0].nickname").value("차단유저A"));
        }
    }
}
