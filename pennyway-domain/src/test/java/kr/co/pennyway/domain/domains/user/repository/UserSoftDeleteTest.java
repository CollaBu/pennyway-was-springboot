package kr.co.pennyway.domain.domains.user.repository;

import jakarta.persistence.EntityManager;
import kr.co.pennyway.domain.config.ContainerMySqlTestConfig;
import kr.co.pennyway.domain.config.JpaConfig;
import kr.co.pennyway.domain.domains.user.domain.User;
import kr.co.pennyway.domain.domains.user.service.UserService;
import kr.co.pennyway.domain.domains.user.type.ProfileVisibility;
import kr.co.pennyway.domain.domains.user.type.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.util.AssertionErrors.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@ContextConfiguration(classes = {JpaConfig.class, UserService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class UserSoftDeleteTest extends ContainerMySqlTestConfig {
    @Autowired
    private UserService userService;

    @Autowired
    private EntityManager em;

    private User user;

    @BeforeEach
    public void setUp() {
        user = User.builder()
                .username("test")
                .name("pannyway")
                .password("test")
                .phone("01012345678")
                .role(Role.USER)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .build();
    }

    @Test
    @DisplayName("유저가 삭제되면 deletedAt이 업데이트된다.")
    @Transactional
    public void deleteUser() {
        // given
        User savedUser = userService.createUser(user);
        Long userId = savedUser.getId();

        // when
        userService.deleteUser(savedUser);
        Object deletedUser = em.createNativeQuery("SELECT * FROM user WHERE id = ?", User.class)
                .setParameter(1, userId)
                .getSingleResult();

        // then
        assertNotNull("유저가 삭제되면 deletedAt이 업데이트된다. ", ((User) deletedUser).getDeletedAt());
        System.out.println("deletedUser = " + deletedUser);
    }

    @Test
    @DisplayName("유저가 삭제되면 findBy와 existsBy로 조회할 수 없다.")
    @Transactional
    public void deleteUserAndFindById() {
        // given
        User savedUser = userService.createUser(user);
        Long userId = savedUser.getId();

        // when
        userService.deleteUser(savedUser);

        // then
        assertFalse("유저가 삭제되면 existsById로 조회할 수 없다. ", userService.isExistUser(userId));
        assertNull("유저가 삭제되면 findById로 조회할 수 없다. ", userService.readUser(userId).orElse(null));
        System.out.println("after delete: savedUser = " + savedUser);
    }
}
