package com.rikkeibank.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.notification.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Service
public class NotificationConsumerService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentLinkedDeque<NotificationEvent> recentNotifications = new ConcurrentLinkedDeque<>();

    @KafkaListener(topics = "transfer-events", groupId = "notification-group")
    public void consumeTransferEvent(String message) {
        try {
            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
            recentNotifications.addFirst(event);
            if (recentNotifications.size() > 50) {
                recentNotifications.removeLast();
            }

            log.info("\n======================================================\n" +
                     "🔔 [RIKKEIBANK NOTIFICATION ALERT - KAFKA CONSUMER]\n" +
                     "   Mã giao dịch : {}\n" +
                     "   Tài khoản gửi: {} (-{} {})\n" +
                     "   Tài khoản nhận: {} (+{} {})\n" +
                     "   Trạng thái   : {}\n" +
                     "   Chi tiết     : {}\n" +
                     "   Thời gian    : {}\n" +
                     "======================================================",
                    event.getTransactionCode(),
                    event.getFromAccount(), event.getAmount(), event.getCurrency(),
                    event.getToAccount(), event.getAmount(), event.getCurrency(),
                    event.getStatus(),
                    event.getMessage(),
                    event.getTimestamp());

        } catch (Exception e) {
            log.error("Lỗi khi đọc sự kiện Kafka: {}", e.getMessage());
        }
    }

    public Flux<NotificationEvent> getRecentNotifications() {
        return Flux.fromIterable(recentNotifications);
    }
}
