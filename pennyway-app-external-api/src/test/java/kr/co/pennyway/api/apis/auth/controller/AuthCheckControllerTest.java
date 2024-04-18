package kr.co.pennyway.api.apis.auth.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import kr.co.pennyway.api.apis.auth.dto.AuthFindDto;
import kr.co.pennyway.api.apis.auth.mapper.AuthFindService;
import kr.co.pennyway.api.config.ExternalApiDBTestConfig;
import kr.co.pennyway.api.config.ExternalApiIntegrationTest;
import kr.co.pennyway.domain.domains.user.exception.UserErrorCode;
import kr.co.pennyway.domain.domains.user.exception.UserErrorException;

@ExternalApiIntegrationTest
@AutoConfigureMockMvc
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class AuthCheckControllerTest extends ExternalApiDBTestConfig {
	private final String inputPhone = "010-1234-5678";
	private final String expectedUsername = "pennyway";

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthFindService authFindMapper;

	@Test
	@DisplayName("일반 회원의 휴대폰 번호로 아이디를 찾을 때 200 응답을 반환한다.")
	void findUsername() throws Exception {
		// given
		given(authFindMapper.findUsername(inputPhone)).willReturn(new AuthFindDto.FindUsernameRes(expectedUsername));

		// when
		ResultActions resultActions = findUsernameRequest(inputPhone);

		// then
		resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.username").value(expectedUsername));
	}

	@Test
	@DisplayName("일반 회원이 아닌 휴대폰 번호로 아이디를 찾을 때 404 응답을 반환한다.")
	void findUsernameIfUserNotFound() throws Exception {
		// given
		String phone = "010-1111-1111";
		given(authFindMapper.findUsername(phone)).willThrow(new UserErrorException(UserErrorCode.NOT_FOUND));

		// when
		ResultActions resultActions = findUsernameRequest(phone);

		// then
		resultActions
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(UserErrorCode.NOT_FOUND.causedBy().getCode()))
				.andExpect(jsonPath("$.message").value(UserErrorCode.NOT_FOUND.getExplainError()));
	}

	private ResultActions findUsernameRequest(String phone) throws Exception {
		return mockMvc.perform(get("/v1/find/username")
				.param("phone", phone));
	}
}