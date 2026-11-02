package team.startup.gwangsan.adapter.web.place.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("지점 HTTP 계약")
class PlaceControllerHttpContractTest {

    @Nested
    @DisplayName("GET /api/place 는")
    class Describe_find_all {

        @Test
        @DisplayName("it_지점 id·name·headId 배열을 반환한다")
        void it_returns_places() throws Exception {
            PlaceQuery placeQuery = mock(PlaceQuery.class);
            when(placeQuery.findAll()).thenReturn(List.of(new PlaceResult(1, "수완지점", 1),
                    new PlaceResult(2, "본부없음", null)));
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new PlaceController(placeQuery)).build();

            mvc.perform(get("/api/place"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("""
                            [{"id":1,"name":"수완지점","headId":1},{"id":2,"name":"본부없음","headId":null}]
                            """, true));
        }
    }
}
