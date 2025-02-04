package kr.co.pennyway.api.apis.chat.service;

import kr.co.pennyway.api.apis.chat.dto.ChatRoomRes;
import kr.co.pennyway.api.apis.chat.mapper.ChatRoomMapper;
import kr.co.pennyway.api.common.storage.AwsS3Adapter;
import kr.co.pennyway.domain.context.account.service.UserService;
import kr.co.pennyway.domain.context.chat.service.ChatMemberService;
import kr.co.pennyway.domain.context.chat.service.ChatMessageService;
import kr.co.pennyway.domain.domains.member.domain.ChatMember;
import kr.co.pennyway.domain.domains.member.dto.ChatMemberResult;
import kr.co.pennyway.domain.domains.member.exception.ChatMemberErrorCode;
import kr.co.pennyway.domain.domains.member.exception.ChatMemberErrorException;
import kr.co.pennyway.domain.domains.member.type.ChatMemberRole;
import kr.co.pennyway.domain.domains.message.domain.ChatMessage;
import kr.co.pennyway.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomWithParticipantsSearchService {
    private static final int MESSAGE_LIMIT = 15;

    private final UserService userService;
    private final ChatMemberService chatMemberService;
    private final ChatMessageService chatMessageService;

    private final AwsS3Adapter awsS3Adapter;

    @Transactional(readOnly = true)
    public ChatRoomRes.RoomWithParticipants execute(Long userId, Long chatRoomId) {
        // 내 정보 조회
        User me = userService.readUser(userId)
                .orElseThrow(() -> new ChatMemberErrorException(ChatMemberErrorCode.NOT_FOUND));

        ChatMember myInfo = chatMemberService.readChatMember(userId, chatRoomId)
                .orElseThrow(() -> new ChatMemberErrorException(ChatMemberErrorCode.NOT_FOUND));
        ChatMemberResult.Detail myDetail = new ChatMemberResult.Detail(myInfo.getId(), me.getName(), myInfo.getRole(), myInfo.isNotifyEnabled(), userId, myInfo.getCreatedAt(), me.getProfileImageUrl());

        // 최근 메시지 조회 (15건)
        List<ChatMessage> chatMessages = chatMessageService.readRecentMessages(chatRoomId, MESSAGE_LIMIT);

        // 최근 메시지의 발신자 조회
        Set<Long> recentParticipantIds = chatMessages.stream()
                .map(ChatMessage::getSender)
                .filter(sender -> !sender.equals(userId))
                .collect(Collectors.toSet());

        // 최근 메시지의 발신자 상세 정보 조회
        List<ChatMemberResult.Detail> recentParticipants = new ArrayList<>(
                chatMemberService.readChatMembersByUserIds(chatRoomId, recentParticipantIds)
        );

        // 내가 관리자가 아니거나, 최근 활동자에 관리자가 없다면 관리자 정보 조회
        if (!myInfo.getRole().equals(ChatMemberRole.ADMIN) && recentParticipants.stream().noneMatch(participant -> participant.role().equals(ChatMemberRole.ADMIN))) {
            ChatMemberResult.Detail admin = chatMemberService.readAdmin(chatRoomId)
                    .orElseThrow(() -> new ChatMemberErrorException(ChatMemberErrorCode.NOT_FOUND));
            recentParticipantIds.add(admin.userId());
            recentParticipants.add(admin);
        }
        recentParticipantIds.add(userId);

        // 채팅방에 속한 다른 사용자 요약 정보 조회
        List<ChatMemberResult.Summary> otherMemberIds = chatMemberService.readChatMemberIdsByUserIdsNotIn(chatRoomId, recentParticipantIds);

        return ChatRoomMapper.toChatRoomResRoomWithParticipants(myDetail, recentParticipants, otherMemberIds, chatMessages, awsS3Adapter.getObjectPrefix());
    }
}
