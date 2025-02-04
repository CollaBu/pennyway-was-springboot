package kr.co.pennyway.batch.processor;

import kr.co.pennyway.batch.common.dto.KeyValue;
import kr.co.pennyway.domain.domains.chatstatus.domain.ChatMessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LastMessageIdProcessor implements ItemProcessor<KeyValue, ChatMessageStatus> {

    @Override
    public ChatMessageStatus process(KeyValue item) throws Exception {
        log.debug("Processing item - key: {}, value: {}", item.key(), item.value());

        String[] parts = item.key().split(":");

        if (parts.length != 4) {
            log.error("Invalid key format: {}", item.key());
            return null;
        }

        try {
            Long roomId = Long.parseLong(parts[2]);
            Long userId = Long.parseLong(parts[3]);
            Long messageId = Long.parseLong(item.value());
            log.debug("Parsed roomId: {}, userId: {}, messageId: {}", roomId, userId, messageId);

            return new ChatMessageStatus(userId, roomId, messageId);
        } catch (NoSuchFieldError | NumberFormatException e) {
            log.error("Failed to parse key: {}", item.key(), e);
            return null;
        }
    }
}
