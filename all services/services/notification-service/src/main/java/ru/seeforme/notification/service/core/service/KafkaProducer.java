package ru.seeforme.notification.service.core.service;

import ru.seeforme.notification.service.api.dto.HelpRequestRequest;

public interface KafkaProducer {

    void sendHelpRequestMessage(HelpRequestRequest helpRequestRequest);
}
