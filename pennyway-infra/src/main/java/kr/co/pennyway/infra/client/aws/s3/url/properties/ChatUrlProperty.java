package kr.co.pennyway.infra.client.aws.s3.url.properties;

import kr.co.pennyway.infra.client.aws.s3.ObjectKeyType;

import java.util.Map;
import java.util.Objects;

public record ChatUrlProperty(
        String imageId,
        String timestamp,
        String ext,
        ObjectKeyType type,
        Long chatroomId,
        Long chatId
) implements PresignedUrlProperty {
    public ChatUrlProperty {
        Objects.requireNonNull(imageId, "이미지 아이디는 필수입니다.");
        Objects.requireNonNull(timestamp, "타임스탬프는 필수입니다.");
        Objects.requireNonNull(ext, "확장자는 필수입니다.");
        Objects.requireNonNull(type, "타입은 필수입니다.");
        Objects.requireNonNull(chatroomId, "채팅방 아이디는 필수입니다.");
        Objects.requireNonNull(chatId, "채팅 아이디는 필수입니다.");
    }

    @Override
    public Map<String, String> variables() {
        return Map.of(
                "chatroom_id", chatroomId.toString(),
                "chat_id", chatId.toString(),
                "uuid", imageId,
                "timestamp", timestamp,
                "ext", ext
        );
    }
}