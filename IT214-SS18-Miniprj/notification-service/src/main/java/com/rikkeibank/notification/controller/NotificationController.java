package com.rikkeibank.notification.controller;

import com.rikkeibank.notification.dto.NotificationEvent;
import com.rikkeibank.notification.service.NotificationConsumerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationConsumerService consumerService;

    @GetMapping(value = "/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<NotificationEvent> getRecentNotifications() {
        return consumerService.getRecentNotifications();
    }
}
