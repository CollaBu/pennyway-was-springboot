package kr.co.pennyway.api.apis.users.usecase;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.co.pennyway.api.apis.users.service.UserDeleteService;
import kr.co.pennyway.api.config.ExternalApiDBTestConfig;
import kr.co.pennyway.api.config.fixture.DeviceTokenFixture;
import kr.co.pennyway.api.config.fixture.UserFixture;
import kr.co.pennyway.domain.config.JpaConfig;
import kr.co.pennyway.domain.domains.device.domain.DeviceToken;
import kr.co.pennyway.domain.domains.device.service.DeviceTokenService;
import kr.co.pennyway.domain.domains.oauth.domain.Oauth;
import kr.co.pennyway.domain.domains.oauth.service.OauthService;
import kr.co.pennyway.domain.domains.oauth.type.Provider;
import kr.co.pennyway.domain.domains.spending.service.SpendingCustomCategoryService;
import kr.co.pennyway.domain.domains.spending.service.SpendingService;
import kr.co.pennyway.domain.domains.user.domain.User;
import kr.co.pennyway.domain.domains.user.exception.UserErrorCode;
import kr.co.pennyway.domain.domains.user.exception.UserErrorException;
import kr.co.pennyway.domain.domains.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.util.AssertionErrors.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@ContextConfiguration(classes = {JpaConfig.class, UserDeleteService.class, UserService.class, OauthService.class, DeviceTokenService.class, SpendingService.class, SpendingCustomCategoryService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserDeleteServiceTest extends ExternalApiDBTestConfig {
    @Autowired
    private UserService userService;

    @Autowired
    private OauthService oauthService;

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private UserDeleteService userDeleteService;

    @Autowired
    private SpendingService spendingService;

    @Autowired
    private SpendingCustomCategoryService spendingCustomCategoryService;

    @MockBean
    private JPAQueryFactory queryFactory;

    @Test
    @Transactional
    @DisplayName("사용자가 삭제된 유저를 조회하려는 경우 NOT_FOUND 에러를 반환한다.")
    void deleteAccountWhenUserIsDeleted() {
        // given
        User user = userService.createUser(UserFixture.GENERAL_USER.toUser());
        userService.deleteUser(user);

        // when - then
        UserErrorException ex = assertThrows(UserErrorException.class, () -> userDeleteService.execute(user.getId()));
        assertEquals("삭제된 사용자인 경우 Not Found를 반환한다.", UserErrorCode.NOT_FOUND, ex.getBaseErrorCode());
    }

    @Test
    @Transactional
    @DisplayName("일반 회원가입 이력만 있는 사용자의 경우, 정상적으로 계정이 삭제된다.")
    void deleteAccount() {
        // given
        User user = UserFixture.GENERAL_USER.toUser();
        userService.createUser(user);

        // when - then
        assertDoesNotThrow(() -> userDeleteService.execute(user.getId()));
        assertTrue("사용자가 삭제되어 있어야 한다.", userService.readUser(user.getId()).isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("사용자 계정 삭제 시, 연동된 모든 소셜 계정은 soft delete 처리되어야 한다.")
    void deleteAccountWithSocialAccounts() {
        // given
        User user = UserFixture.OAUTH_USER.toUser();
        userService.createUser(user);

        Oauth kakao = createOauth(Provider.KAKAO, "kakaoId", user);
        Oauth google = createOauth(Provider.GOOGLE, "googleId", user);

        // when - then
        assertDoesNotThrow(() -> userDeleteService.execute(user.getId()));

        assertTrue("사용자가 삭제되어 있어야 한다.", userService.readUser(user.getId()).isEmpty());
        assertTrue("카카오 계정이 삭제되어 있어야 한다.", oauthService.readOauth(kakao.getId()).get().isDeleted());
        assertTrue("구글 계정이 삭제되어 있어야 한다.", oauthService.readOauth(google.getId()).get().isDeleted());
    }

    @Test
    @Transactional
    @DisplayName("사용자 삭제 시, 디바이스 정보는 비활성화되어야 한다.")
    void deleteAccountWithDevices() {
        // given
        User user = UserFixture.GENERAL_USER.toUser();
        userService.createUser(user);

        DeviceToken deviceToken = DeviceTokenFixture.INIT.toDevice(user);
        deviceTokenService.createDevice(deviceToken);

        // when - then
        assertDoesNotThrow(() -> userDeleteService.execute(user.getId()));
        assertTrue("사용자가 삭제되어 있어야 한다.", userService.readUser(user.getId()).isEmpty());
        assertFalse("디바이스가 비활성화 있어야 한다.", deviceTokenService.readDeviceByUserIdAndToken(user.getId(), deviceToken.getToken()).get().getActivated());
    }

    private Oauth createOauth(Provider provider, String providerId, User user) {
        Oauth oauth = Oauth.of(provider, providerId, user);
        return oauthService.createOauth(oauth);
    }
}
