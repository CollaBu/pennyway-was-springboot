package kr.co.pennyway.socket.common.registry;

import kr.co.pennyway.domain.domains.member.service.ChatMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component("chatRoomAccessChecker")
@RequiredArgsConstructor
public class ChatRoomAccessChecker implements ResourceAccessChecker {
    private final ChatMemberService chatMemberService;

    @Override
    public boolean hasPermission(String path, Principal principal) {
        return isChatRoomAccess(getChatRoomId(path), principal);
    }

    /**
     * path에서 chatRoomId를 추출한다.
     *
     * @param path : {@code /sub/chat.room.{roomId} 포맷}
     * @return chatRoomId
     */
    private Long getChatRoomId(String path) {
        String[] split = path.split("\\.");
        return Long.parseLong(split[split.length - 1]);
    }

    private boolean isChatRoomAccess(Long chatRoomId, Principal principal) {
        return chatMemberService.isExists(chatRoomId, Long.parseLong(principal.getName()));
    }
}