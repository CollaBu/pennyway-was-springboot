package kr.co.pennyway.api.apis.ledger.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pennyway.api.apis.ledger.dto.SpendingReq;
import kr.co.pennyway.api.config.ExternalApiDBTestConfig;
import kr.co.pennyway.api.config.ExternalApiIntegrationTest;
import kr.co.pennyway.api.config.fixture.SpendingFixture;
import kr.co.pennyway.api.config.fixture.UserFixture;
import kr.co.pennyway.api.config.supporter.WithSecurityMockUser;
import kr.co.pennyway.domain.domains.spending.type.SpendingCategory;
import kr.co.pennyway.domain.domains.user.domain.User;
import kr.co.pennyway.domain.domains.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExternalApiIntegrationTest
@AutoConfigureMockMvc
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class SpendingUseCaseIntegrationTest extends ExternalApiDBTestConfig {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Order(1)
    @Nested
    @DisplayName("지출 내역 추가하기")
    class CreateSpending {
        @Test
        @DisplayName("request의 categoryId가 -1인 경우, spendingCustomCategory가 null인 Spending을 생성한다.")
        @WithSecurityMockUser(userId = "1")
        void createSpendingSuccess() throws Exception {
            // given
            userService.createUser(UserFixture.GENERAL_USER.toUser());
            SpendingReq request = new SpendingReq(10000, -1L, SpendingCategory.FOOD, LocalDate.now(), "소비처", "메모");

            // when
            ResultActions resultActions = performCreateSpendingSuccess(request);

            // then
            resultActions.andDo(print()).andExpect(status().isOk());
        }

        private ResultActions performCreateSpendingSuccess(SpendingReq req) throws Exception {
            return mockMvc.perform(MockMvcRequestBuilders
                    .post("/v2/spendings")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(req)));
        }
    }

    @Order(2)
    @Nested
    @DisplayName("월별 지출 내역 조회")
    class GetSpendingListAtYearAndMonth {
        @Test
        @DisplayName("월별 지출 내역 조회")
        @WithSecurityMockUser(userId = "2")
        void getSpendingListAtYearAndMonthSuccess() throws Exception {
            // given
            User user = userService.createUser(UserFixture.GENERAL_USER.toUser());
            SpendingFixture.bulkInsertSpending(user, 150, jdbcTemplate);

            // when
            long before = System.currentTimeMillis();
            ResultActions resultActions = performGetSpendingListAtYearAndMonthSuccess();
            long after = System.currentTimeMillis();

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isOk());
            log.debug("수행 시간: {}ms", after - before);
        }

        private ResultActions performGetSpendingListAtYearAndMonthSuccess() throws Exception {
            LocalDate now = LocalDate.now();
            return mockMvc.perform(MockMvcRequestBuilders.get("/v2/spendings")
                    .param("year", String.valueOf(now.getYear()))
                    .param("month", String.valueOf(now.getMonthValue())));
        }
    }
}
