package kr.co.pennyway.batch.writer;

import kr.co.pennyway.batch.dto.DailySpendingNotification;
import kr.co.pennyway.domain.domains.device.dto.DeviceTokenOwner;
import kr.co.pennyway.domain.domains.notification.repository.NotificationRepository;
import kr.co.pennyway.domain.domains.notification.type.Announcement;
import kr.co.pennyway.infra.common.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationWriter implements ItemWriter<DeviceTokenOwner> {
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public void write(@NonNull Chunk<? extends DeviceTokenOwner> owners) throws Exception {
        LocalDateTime publishedAt = LocalDateTime.now();

        List<Long> userIds = new ArrayList<>();
        Map<Long, DailySpendingNotification> notificationMap = new HashMap<>();

        for (DeviceTokenOwner owner : owners) {
            userIds.add(owner.userId());
            notificationMap.computeIfAbsent(owner.userId(), k -> DailySpendingNotification.from(owner)).addDeviceToken(owner.deviceToken());
        }

        notificationRepository.saveDailySpendingAnnounceInBulk(userIds, publishedAt, Announcement.DAILY_SPENDING);

        for (DailySpendingNotification notification : notificationMap.values()) {
            publisher.publishEvent(NotificationEvent.of(notification.title(), notification.content(), notification.deviceTokens(), ""));
        }
    }
}
