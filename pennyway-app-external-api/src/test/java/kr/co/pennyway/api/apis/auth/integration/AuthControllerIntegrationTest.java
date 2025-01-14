package kr.co.pennyway.api.apis.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pennyway.api.apis.auth.dto.PhoneVerificationDto;
import kr.co.pennyway.api.apis.auth.dto.SignUpReq;
import kr.co.pennyway.api.common.exception.PhoneVerificationErrorCode;
import kr.co.pennyway.api.config.ExternalApiDBTestConfig;
import kr.co.pennyway.api.config.ExternalApiIntegrationTest;
import kr.co.pennyway.api.config.fixture.UserFixture;
import kr.co.pennyway.domain.context.account.service.OauthService;
import kr.co.pennyway.domain.context.account.service.PhoneCodeService;
import kr.co.pennyway.domain.context.account.service.UserService;
import kr.co.pennyway.domain.domains.oauth.domain.Oauth;
import kr.co.pennyway.domain.domains.oauth.type.Provider;
import kr.co.pennyway.domain.domains.phone.type.PhoneCodeKeyType;
import kr.co.pennyway.domain.domains.user.domain.User;
import kr.co.pennyway.domain.domains.user.exception.UserErrorCode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExternalApiIntegrationTest
@AutoConfigureMockMvc
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class AuthControllerIntegrationTest extends ExternalApiDBTestConfig {
    private final String expectedPhone = "010-1234-5678";
    private final String expectedCode = "123456";
    private final String expectedDeviceId = "AA-BB-CC-DD";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @SpyBean
    private PhoneCodeService phoneCodeService;
    @SpyBean
    private UserService userService;
    @Autowired
    private OauthService oauthService;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext) {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .defaultRequest(post("/**").with(csrf()))
                .build();
    }

    @Nested
    @Order(1)
    @DisplayName("[2] 전화번호 검증 테스트")
    class GeneralSignUpPhoneVerifyTest {
        @Test
        @WithAnonymousUser
        @DisplayName("일반 회원가입 이력이 있는 경우 409 Conflict를 반환하고, 인증 코드 캐시 데이터가 제거된다.")
        void generalSignUpFailBecauseAlreadyGeneralSignUp() throws Exception {
            // given
            phoneCodeService.create(expectedPhone, expectedCode, PhoneCodeKeyType.SIGN_UP);
            given(userService.readUserByPhone(expectedPhone)).willReturn(Optional.of(UserFixture.GENERAL_USER.toUser()));

            // when
            ResultActions resultActions = performPhoneVerificationRequest(expectedCode);

            // then
            resultActions
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(UserErrorCode.ALREADY_SIGNUP.causedBy().getCode()))
                    .andExpect(jsonPath("$.message").value(UserErrorCode.ALREADY_SIGNUP.getExplainError()))
                    .andDo(print());
            assertThrows(IllegalArgumentException.class, () -> phoneCodeService.readByPhone(expectedPhone, PhoneCodeKeyType.SIGN_UP));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("인증 번호가 일치하지 않는 경우 401 UNAUTHORIZED를 반환한다.")
        void generalSignUpFailBecauseInvalidCode() throws Exception {
            // given
            phoneCodeService.create(expectedPhone, expectedCode, PhoneCodeKeyType.SIGN_UP);
            given(userService.readUserByPhone(expectedPhone)).willReturn(Optional.empty());
            String invalidCode = "111111";

            // when
            ResultActions resultActions = performPhoneVerificationRequest(invalidCode);

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(PhoneVerificationErrorCode.IS_NOT_VALID_CODE.causedBy().getCode()))
                    .andExpect(jsonPath("$.message").value(PhoneVerificationErrorCode.IS_NOT_VALID_CODE.getExplainError()))
                    .andDo(print());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("일치하는 전화번호 혹은 인증 번호가 없는 경우 404 NOT_FOUND를 반환한다.")
        void generalSignUpFailBecauseNotFound() throws Exception {
            // given
            given(userService.readUserByPhone(expectedPhone)).willReturn(Optional.empty());

            // when
            ResultActions resultActions = performPhoneVerificationRequest(expectedCode);

            // then
            resultActions
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(PhoneVerificationErrorCode.EXPIRED_OR_INVALID_PHONE.causedBy().getCode()))
                    .andExpect(jsonPath("$.message").value(PhoneVerificationErrorCode.EXPIRED_OR_INVALID_PHONE.getExplainError()))
                    .andDo(print());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("소셜 로그인 이력이 없는 경우, 200 OK를 반환하고 oauth 필드가 false이다.")
        void generalSignUpSuccess() throws Exception {
            // given
            phoneCodeService.create(expectedPhone, expectedCode, PhoneCodeKeyType.SIGN_UP);
            given(userService.readUserByPhone(expectedPhone)).willReturn(Optional.empty());

            // when
            ResultActions resultActions = performPhoneVerificationRequest(expectedCode);

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.sms.code").value(true))
                    .andExpect(jsonPath("$.data.sms.oauth").value(false))
                    .andDo(print());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("소셜 로그인 이력이 있는 경우, 200 OK를 반환하고 oauth 필드가 true고 username 필드가 존재한다.")
        void generalSignUpSuccessWithOauth() throws Exception {
            // given
            phoneCodeService.create(expectedPhone, expectedCode, PhoneCodeKeyType.SIGN_UP);
            given(userService.readUserByPhone(expectedPhone)).willReturn(Optional.of(UserFixture.OAUTH_USER.toUser()));

            // when
            ResultActions resultActions = performPhoneVerificationRequest(expectedCode);

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.sms.code").value(true))
                    .andExpect(jsonPath("$.data.sms.oauth").value(true))
                    .andExpect(jsonPath("$.data.sms.username").value(UserFixture.OAUTH_USER.getUsername()))
                    .andDo(print());
        }

        @AfterEach
        void tearDown() {
            phoneCodeService.delete(expectedPhone, PhoneCodeKeyType.SIGN_UP);
        }

        private ResultActions performPhoneVerificationRequest(String expectedCode) throws Exception {
            PhoneVerificationDto.VerifyCodeReq request = new PhoneVerificationDto.VerifyCodeReq(expectedPhone, expectedCode);
            return mockMvc.perform(
                    post("/v1/auth/phone/verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );
        }
    }

    @Nested
    @Order(2)
    @DisplayName("[3-1] 일반 회원가입 테스트")
    class GeneralSignUpTest {
        @Test
        @WithAnonymousUser
        @DisplayName("인증번호가 일치하지 않는 경우 401 UNAUTHORIZED를 반환한다.")
        void generalSignUpFailBecauseInvalidCode() throws Exception {
            // given
            phoneCodeService.create(expectedPhone, expectedCode, PhoneCodeKeyType.SIGN_UP);
            String invalidCode = "111111";

            // when
            ResultActions resultActions = performGeneralSignUpRequest(invalidCode);

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(PhoneVerificationErrorCode.IS_NOT_VALID_CODE.causedBy().getCode()))
                    .andExpect(jsonPath("$.message").value(PhoneVerificationErrorCode.IS_NOT_VALID_CODE.getExplainError()))
                    .andDo(print());
        }

        @Test
        @WithAnonymousUser
        @Transactional
        @DisplayName("인증번호가 일치하는 경우 200 OK를 반환하고, 회원가입이 완료된다.")
        void generalSignUpSuccess() throws Exception {
            // given
            phoneCodeService.create(expectedPhone, expectedCode, PhoneCodeKeyType.SIGN_UP);
            given(userService.readUserByPhone(expectedPhone)).willReturn(Optional.empty());

            // when
            ResultActions resultActions = performGeneralSignUpRequest(expectedCode);

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Set-Cookie"))
                    .andExpect(header().exists("Authorization"))
                    .andExpect(jsonPath("$.data.user.id").value(1))
                    .andDo(print());
        }

        private ResultActions performGeneralSignUpRequest(String code) throws Exception {
            SignUpReq.General request = new SignUpReq.General(UserFixture.GENERAL_USER.getUsername(), "pennyway", "dkssudgktpdy1", expectedPhone, code, expectedDeviceId);
            return mockMvc.perform(
                    post("/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );
        }

        @AfterEach
        void tearDown() {
            phoneCodeService.delete(expectedPhone, PhoneCodeKeyType.SIGN_UP);
        }
    }

    @Nested
    @Order(3)
    @DisplayName("[3-2] 소셜 계정 연동 회원가입 테스트")
    class SyncWithOauthSignUpTest {
        @Test
        @WithAnonymousUser
        @DisplayName("인증번호가 일치하지 않는 경우 401 UNAUTHORIZED를 반환한다.")
        void syncWithOauthSignUpFailBecauseInvalidCode() throws Exception {
            // given
            phoneCodeService.create(expectedPhone, expectedCode, PhoneCodeKeyType.SIGN_UP);
            String invalidCode = "111111";

            // when
            ResultActions resultActions = performSyncWithOauthSignUpRequest(expectedPhone, invalidCode);

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(PhoneVerificationErrorCode.IS_NOT_VALID_CODE.causedBy().getCode()))
                    .andExpect(jsonPath("$.message").value(PhoneVerificationErrorCode.IS_NOT_VALID_CODE.getExplainError()))
                    .andDo(print());
        }

        @Test
        @WithAnonymousUser
        @Transactional
        @DisplayName("인증번호가 일치하는 경우 200 OK를 반환하고, 기존의 소셜 계정과 연동된 회원가입이 완료된다.")
        void syncWithOauthSignUpSuccess() throws Exception {
            // given
            User user = userService.createUser(UserFixture.OAUTH_USER.toUser());
            oauthService.createOauth(Oauth.of(Provider.KAKAO, "oauthId", user));
            phoneCodeService.create(user.getPhone(), expectedCode, PhoneCodeKeyType.SIGN_UP);

            // when
            ResultActions resultActions = performSyncWithOauthSignUpRequest(user.getPhone(), expectedCode);

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Set-Cookie"))
                    .andExpect(header().exists("Authorization"))
                    .andExpect(jsonPath("$.data.user.id").value(user.getId()))
                    .andDo(print());
            assertNotNull(oauthService.readOauthByOauthIdAndProvider("oauthId", Provider.KAKAO));
            assertNotNull(user.getPassword());
        }

        private ResultActions performSyncWithOauthSignUpRequest(String expectedPhone, String code) throws Exception {
            SignUpReq.SyncWithOauth request = new SignUpReq.SyncWithOauth("dkssudgktpdy1", expectedPhone, code, expectedDeviceId);
            return mockMvc.perform(
                    post("/v1/auth/link-oauth")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );
        }

        @AfterEach
        void tearDown() {
            phoneCodeService.delete(expectedPhone, PhoneCodeKeyType.SIGN_UP);
        }
    }
}