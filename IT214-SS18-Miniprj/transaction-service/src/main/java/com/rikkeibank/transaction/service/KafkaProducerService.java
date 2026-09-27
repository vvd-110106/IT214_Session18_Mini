package com.rikkeibank.transaction.service;

import com.rikkeibank.transaction.event.TransferEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducerService {

    public static final String TOPIC_TRANSFER_EVENTS = "transfer-events";

    private final KafkaTemplate<String, TransferEvent> kafkaTemplate;

    @Autowired
    public KafkaProducerService(@Autowired(required = false) KafkaTemplate<String, TransferEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransferEvent(TransferEvent event) {
        if (kafkaTemplate != null) {
            try {
                log.info("--> [KAFKA PRODUCER] Đang phát hành sự kiện chuyển khoản lên Topic '{}': TxnCode={}, Status={}",
                        TOPIC_TRANSFER_EVENTS, event.getTransactionCode(), event.getStatus());
                kafkaTemplate.send(TOPIC_TRANSFER_EVENTS, event.getTransactionCode(), event);
            } catch (Exception e) {
                log.warn("--> [KAFKA PRODUCER ERROR] Không thể gửi message lên Kafka broker: {}", e.getMessage());
            }
        } else {
            log.info("--> [KAFKA PRODUCER MOCK] KafkaTemplate chưa được cấu hình. Sự kiện đã ghi nhận: {}", event);
        }
    }
}
