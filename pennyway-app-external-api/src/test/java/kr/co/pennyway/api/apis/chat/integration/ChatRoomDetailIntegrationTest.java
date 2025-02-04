package kr.co.pennyway.api.apis.chat.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pennyway.api.apis.chat.dto.ChatRoomRes;
import kr.co.pennyway.api.common.response.SuccessResponse;
import kr.co.pennyway.api.common.util.ApiTestHelper;
import kr.co.pennyway.api.config.ExternalApiDBTestConfig;
import kr.co.pennyway.api.config.ExternalApiIntegrationTest;
import kr.co.pennyway.api.config.fixture.ChatRoomFixture;
import kr.co.pennyway.api.config.fixture.UserFixture;
import kr.co.pennyway.domain.domains.chatroom.domain.ChatRoom;
import kr.co.pennyway.domain.domains.chatroom.repository.ChatRoomRepository;
import kr.co.pennyway.domain.domains.member.domain.ChatMember;
import kr.co.pennyway.domain.domains.member.repository.ChatMemberRepository;
import kr.co.pennyway.domain.domains.member.type.ChatMemberRole;
import kr.co.pennyway.domain.domains.message.domain.ChatMessage;
import kr.co.pennyway.domain.domains.message.domain.ChatMessageBuilder;
import kr.co.pennyway.domain.domains.message.repository.ChatMessageRepositoryImpl;
import kr.co.pennyway.domain.domains.message.type.MessageCategoryType;
import kr.co.pennyway.domain.domains.message.type.MessageContentType;
import kr.co.pennyway.domain.domains.user.domain.User;
import kr.co.pennyway.domain.domains.user.repository.UserRepository;
import kr.co.pennyway.infra.client.guid.IdGenerator;
import kr.co.pennyway.infra.common.jwt.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ExternalApiIntegrationTest
public class ChatRoomDetailIntegrationTest extends ExternalApiDBTestConfig {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtProvider accessTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMemberRepository chatMemberRepository;

    @Autowired
    private ChatMessageRepositoryImpl chatMessageRepository;

    @Autowired
    private IdGenerator<Long> idGenerator;

    private ApiTestHelper apiTestHelper;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        apiTestHelper = new ApiTestHelper(restTemplate, objectMapper, accessTokenProvider);
    }

    @AfterEach
    void tearDown() {
        Set<String> keys = redisTemplate.keys("chatroom:*:message");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        chatMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Happy Path: 사용자는 채팅방 상세 정보를 조회할 수 있다.")
    void successGetChatRoomDetail() {
        // given
        var owner = userRepository.save(UserFixture.GENERAL_USER.toUser());
        var chatRoom = chatRoomRepository.save(ChatRoomFixture.PUBLIC_CHAT_ROOM.toEntity(1L));
        var ownerMember = chatMemberRepository.save(ChatMember.of(owner, chatRoom, ChatMemberRole.ADMIN));

        User member = userRepository.save(UserFixture.GENERAL_USER.toUser());
        ChatMember participant = chatMemberRepository.save(ChatMember.of(member, chatRoom, ChatMemberRole.MEMBER));

        int expectedRecentParticipantCount = 1; // 나 자신은 제외
        int expectedMessageCount = 5;

        for (int i = 1; i <= 5; i++) {
            chatMessageRepository.save(createTestMessage(chatRoom.getId(), (long) i, i % 2 == 0 ? owner.getId() : participant.getId()));
        }

        // when
        ResponseEntity<?> response = performApi(owner, chatRoom.getId());

        // then
        assertAll(
                () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                () -> {
                    SuccessResponse<Map<String, ChatRoomRes.RoomWithParticipants>> result = (SuccessResponse<Map<String, ChatRoomRes.RoomWithParticipants>>) response.getBody();
                    ChatRoomRes.RoomWithParticipants payload = result.getData().get("chatRoom");

                    assertNotNull(result);
                    assertEquals(owner.getId(), payload.myInfo().id(), "내 ID가 일치해야 한다");
                    assertEquals(ownerMember.getRole(), ChatMemberRole.ADMIN, "나는 방장 권한이어야 한다");
                    assertEquals(expectedRecentParticipantCount, payload.recentParticipants().size(), "최근 참여자 개수가 일치해야 한다");
                    assertEquals(expectedMessageCount, payload.recentMessages().size(), "최근 메시지 개수가 일치해야 한다");
                    assertTrue(payload.otherParticipants().isEmpty(), "다른 참여자가 없어야 한다");
                }
        );
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 사용자는 조회할 수 없다")
    void failGetChatRoomDetailWhenNotMember() {
        var owner = userRepository.save(UserFixture.GENERAL_USER.toUser());
        var chatRoom = chatRoomRepository.save(ChatRoomFixture.PUBLIC_CHAT_ROOM.toEntity(2L));
        var ownerMember = chatMemberRepository.save(ChatMember.of(owner, chatRoom, ChatMemberRole.ADMIN));

        // given
        User nonMember = userRepository.save(UserFixture.GENERAL_USER.toUser());

        // when
        ResponseEntity<?> response = performApi(nonMember, chatRoom.getId());

        // then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "채팅방 멤버가 아닌 사용자는 조회할 수 없어야 한다");
    }

    @Test
    @DisplayName("최근 메시지가 없는 채팅방도 정상적으로 조회된다")
    void successGetChatRoomDetailWithoutMessages() {
        // given
        var owner = userRepository.save(UserFixture.GENERAL_USER.toUser());
        var chatRoom = chatRoomRepository.save(ChatRoomFixture.PUBLIC_CHAT_ROOM.toEntity(3L));
        var ownerMember = chatMemberRepository.save(ChatMember.of(owner, chatRoom, ChatMemberRole.ADMIN));

        var member = userRepository.save(UserFixture.GENERAL_USER.toUser());
        var participant = chatMemberRepository.save(ChatMember.of(member, chatRoom, ChatMemberRole.MEMBER));

        // when
        ResponseEntity<?> response = performApi(member, chatRoom.getId());

        // then
        SuccessResponse<Map<String, ChatRoomRes.RoomWithParticipants>> result = (SuccessResponse<Map<String, ChatRoomRes.RoomWithParticipants>>) response.getBody();
        ChatRoomRes.RoomWithParticipants payload = result.getData().get("chatRoom");

        assertNotNull(result);
        assertTrue(payload.recentMessages().isEmpty(), "최근 메시지가 없어야 한다");
    }

    @Test
    @DisplayName("채팅방에 다수의 참여자가 있는 경우 정상적으로 조회된다")
    void successGetChatRoomDetailWithManyParticipants() {
        // given
        var owner = userRepository.save(UserFixture.GENERAL_USER.toUser());
        var chatRoom = chatRoomRepository.save(ChatRoomFixture.PUBLIC_CHAT_ROOM.toEntity(4L));
        var ownerMember = chatMemberRepository.save(ChatMember.of(owner, chatRoom, ChatMemberRole.ADMIN));

        int expectedParticipantCount = 10;
        List<User> participants = createMultipleParticipants(expectedParticipantCount, chatRoom);

        chatMessageRepository.save(createTestMessage(chatRoom.getId(), 1L, owner.getId()));
        chatMessageRepository.save(createTestMessage(chatRoom.getId(), 2L, participants.get(0).getId()));
        chatMessageRepository.save(createTestMessage(chatRoom.getId(), 3L, participants.get(1).getId()));

        // when
        ResponseEntity<?> response = performApi(owner, chatRoom.getId());

        // then
        SuccessResponse<Map<String, ChatRoomRes.RoomWithParticipants>> result = (SuccessResponse<Map<String, ChatRoomRes.RoomWithParticipants>>) response.getBody();
        ChatRoomRes.RoomWithParticipants payload = result.getData().get("chatRoom");

        assertAll(
                () -> assertNotNull(payload),
                () -> assertEquals(payload.recentParticipants().size(), 2, "최근 참여자 개수가 일치해야 한다."),
                () -> assertEquals(payload.otherParticipants().size(), expectedParticipantCount - 2, "다른 참여자 개수가 일치해야 한다.")
        );
    }

    private ChatMessage createTestMessage(Long chatRoomId, Long idx, Long senderId) {
        return ChatMessageBuilder.builder()
                .chatRoomId(chatRoomId)
                .chatId(idGenerator.generate())
                .content("Test message " + idx)
                .contentType(MessageContentType.TEXT)
                .categoryType(MessageCategoryType.NORMAL)
                .sender(senderId)
                .build();
    }

    private List<User> createMultipleParticipants(int count, ChatRoom chatRoom) {
        List<User> participants = new ArrayList<>();

        for (int i = 0; i < count; ++i) {
            var user = userRepository.save(UserFixture.GENERAL_USER.toUser());
            chatMemberRepository.save(ChatMember.of(user, chatRoom, ChatMemberRole.MEMBER));
            participants.add(user);
        }

        return participants;
    }

    private ResponseEntity<?> performApi(User user, Long roomId) {
        return apiTestHelper.callApi(
                "http://localhost:" + port + "/v2/chat-rooms/{roomId}",
                HttpMethod.GET,
                user,
                null,
                new TypeReference<SuccessResponse<Map<String, ChatRoomRes.RoomWithParticipants>>>() {
                },
                roomId
        );
    }
}
