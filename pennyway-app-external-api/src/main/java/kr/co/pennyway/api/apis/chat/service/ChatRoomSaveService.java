package kr.co.pennyway.api.apis.chat.service;

import kr.co.pennyway.api.apis.chat.dto.ChatRoomReq;
import kr.co.pennyway.domain.common.redis.chatroom.PendedChatRoom;
import kr.co.pennyway.domain.common.redis.chatroom.PendedChatRoomErrorCode;
import kr.co.pennyway.domain.common.redis.chatroom.PendedChatRoomErrorException;
import kr.co.pennyway.domain.common.redis.chatroom.PendedChatRoomService;
import kr.co.pennyway.domain.domains.chatroom.domain.ChatRoom;
import kr.co.pennyway.domain.domains.chatroom.service.ChatRoomService;
import kr.co.pennyway.domain.domains.member.domain.ChatMember;
import kr.co.pennyway.domain.domains.member.service.ChatMemberService;
import kr.co.pennyway.domain.domains.member.type.ChatMemberRole;
import kr.co.pennyway.domain.domains.user.domain.User;
import kr.co.pennyway.domain.domains.user.exception.UserErrorCode;
import kr.co.pennyway.domain.domains.user.exception.UserErrorException;
import kr.co.pennyway.domain.domains.user.service.UserService;
import kr.co.pennyway.infra.client.guid.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomSaveService {
    private final UserService userService;
    private final ChatRoomService chatRoomService;
    private final ChatMemberService chatMemberService;
    private final PendedChatRoomService pendedChatRoomService;

    private final IdGenerator<Long> idGenerator;

    /**
     * 채팅방 생성 정보를 캐싱한다.
     * 해당 요청은 {@link #createChatRoom(ChatRoomReq.Pend, Long)}를 통해 채팅방을 확정할 수 있다.
     *
     * @return 캐싱된 채팅방의 ID
     */
    @Transactional
    public Long pendChatRoom(ChatRoomReq.Pend request, Long userId) {
        Long chatRoomId = idGenerator.generate();
        pendedChatRoomService.create(request.toEntity(chatRoomId, userId));

        return chatRoomId;
    }

    /**
     * 캐싱된 채팅방을 확정하고 채팅방을 생성한다.
     * 채팅방을 생성한 사용자는 채팅방의 관리자로 설정된다.
     *
     * @throws PendedChatRoomErrorException: <br>
     *                                       - {@link PendedChatRoomErrorCode#NOT_FOUND} - 캐싱된 채팅방이 존재하지 않을 경우 <br>
     *                                       - {@link PendedChatRoomErrorCode#INVALID_CREATOR} - 채팅방 정보를 생성한 사용자가 아닐 경우
     */
    @Transactional
    public ChatRoom createChatRoom(ChatRoomReq.Create request, Long userId) {
        PendedChatRoom pendedChatRoom = pendedChatRoomService.readByUserId(userId)
                .orElseThrow(() -> new PendedChatRoomErrorException(PendedChatRoomErrorCode.NOT_FOUND));

        if (!pendedChatRoom.getUserId().equals(userId)) {
            throw new PendedChatRoomErrorException(PendedChatRoomErrorCode.INVALID_CREATOR);
        }

        ChatRoom chatRoom = chatRoomService.create(pendedChatRoom.toChatRoom(request.backgroundImageUrl()));

        User user = userService.readUser(userId).orElseThrow(() -> new UserErrorException(UserErrorCode.NOT_FOUND));
        ChatMember member = ChatMember.of(user.getName(), user, chatRoom, ChatMemberRole.ADMIN);

        chatMemberService.create(member);

        return chatRoom;
    }
}
