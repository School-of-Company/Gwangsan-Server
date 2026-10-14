package team.startup.gwangsan.adapter.web.admin.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team.startup.gwangsan.adapter.integration.admin.AdminProductBridge;
import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.data.AdminProductSearch;
import team.startup.gwangsan.application.admin.port.in.FindAdminProductsQuery;
import team.startup.gwangsan.application.admin.service.DeleteAdminProductService;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.post.port.in.ProductStatusUseCase;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.adapter.security.auth.MemberDetailsService;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;
import team.startup.gwangsan.infrastructure.security.SecurityConfig;
import team.startup.gwangsan.infrastructure.security.JwtAccessDeniedHandler;
import team.startup.gwangsan.infrastructure.security.JwtAuthenticationEntryPoint;
import team.startup.gwangsan.adapter.security.auth.JwtProvider;
import team.startup.gwangsan.adapter.security.auth.TokenParser;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminProductController.class)
@ContextConfiguration(classes = {AdminProductController.class, DeleteAdminProductService.class, AdminProductBridge.class,
        SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
        GlobalExceptionHandler.class})
@DisplayName("관리자 상품 API 실제 보안 필터 통합 테스트")
class AdminProductControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean FindAdminProductsQuery findAdminProductsService;
    @MockitoBean ProductSnapshotQuery productSnapshotQuery;
    @MockitoBean ProductImageQuery productImageQuery;
    @MockitoBean ProductStatusUseCase productRepository;
    @MockitoBean MemberSummaryQuery memberSummaryQuery;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean TokenParser tokenParser;
    @MockitoBean MemberDetailsService memberDetailsService;

    @Nested
    @DisplayName("관리자 접근과 삭제")
    class AdminAccess {
        @ParameterizedTest
        @ValueSource(strings = {"PLACE_ADMIN", "HEAD_ADMIN"})
        void it_allows_both_admin_roles_to_list_and_delete_another_members_product(String role) throws Exception {
            when(findAdminProductsService.query(new AdminProductSearch(null, null, null, 10))).thenReturn(List.of());

            mvc.perform(get("/api/admin/product").with(user("관리자").roles(role)))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));
            mvc.perform(delete("/api/admin/product/7").with(user("관리자").roles(role)))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            // 작성자·역할과 무관하게 post 공개 command 의 관리자 삭제(논리 삭제)로 위임한다.
            verify(productRepository).deleteByAdmin(7L);
        }

        @Test
        void it_returns_not_found_for_missing_or_deleted_product() throws Exception {
            doThrow(new NotFoundProductException()).when(productRepository).deleteByAdmin(7L);
            mvc.perform(delete("/api/admin/product/7").with(user("관리자").roles("HEAD_ADMIN")))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("status").value(404));
        }

        @Test
        void it_binds_optional_filters_and_exclusive_cursor() throws Exception {
            var response = new AdminProduct(7L, "게시글", "설명", 5000, "OBJECT", "RECEIVER",
                    new AdminProduct.Owner(3L, "작성자", "다른 지점", 3), List.of(), false, true);
            when(findAdminProductsService.query(new AdminProductSearch("OBJECT", "RECEIVER", 8L, 2))).thenReturn(List.of(response));
            mvc.perform(get("/api/admin/product").param("type", "OBJECT").param("mode", "RECEIVER")
                            .param("last_id", "8").param("size", "2").with(user("관리자").roles("PLACE_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(7))
                    .andExpect(jsonPath("$[0].type").value("OBJECT"))
                    .andExpect(jsonPath("$[0].mode").value("RECEIVER"))
                    .andExpect(jsonPath("$[0].member.placeName").value("다른 지점"))
                    .andExpect(jsonPath("$[0].images").isEmpty())
                    .andExpect(jsonPath("$[0].isReserved").value(true));
            verify(findAdminProductsService).query(new AdminProductSearch("OBJECT", "RECEIVER", 8L, 2));
        }
    }

    @Nested
    @DisplayName("접근 거부")
    class DeniedAccess {
        @Test
        void it_rejects_regular_members_without_calling_services() throws Exception {
            mvc.perform(get("/api/admin/product").with(user("회원").roles("USER")))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/admin/product/7").with(user("회원").roles("USER")))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(findAdminProductsService, productRepository);
        }

        @Test
        void it_rejects_anonymous_requests_without_calling_services() throws Exception {
            mvc.perform(get("/api/admin/product")).andExpect(status().isUnauthorized());
            mvc.perform(delete("/api/admin/product/7")).andExpect(status().isUnauthorized());
            verifyNoInteractions(findAdminProductsService, productRepository);
        }
    }

    @Nested
    @DisplayName("입력 검증")
    class Validation {
        @ParameterizedTest
        @CsvSource({"type,INVALID", "mode,INVALID", "size,0", "size,-1", "size,abc",
                "size,2147483648", "last_id,0", "last_id,-1", "last_id,abc", "last_id,9223372036854775808"})
        void it_rejects_invalid_query_parameters(String name, String value) throws Exception {
            mvc.perform(get("/api/admin/product").param(name, value).with(user("관리자").roles("HEAD_ADMIN")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(findAdminProductsService, productRepository);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
        void it_rejects_invalid_product_ids(String id) throws Exception {
            mvc.perform(delete("/api/admin/product/" + id).with(user("관리자").roles("PLACE_ADMIN")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(productRepository);
        }
    }
}
