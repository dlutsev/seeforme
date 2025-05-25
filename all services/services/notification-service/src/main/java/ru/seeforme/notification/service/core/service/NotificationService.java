package ru.seeforme.notification.service.core.service;

import ru.seeforme.notification.service.api.dto.KafkaHelpRequest;

public interface NotificationService {

    void sendHelpRequestNotification(KafkaHelpRequest kafkaHelpRequest);
}
