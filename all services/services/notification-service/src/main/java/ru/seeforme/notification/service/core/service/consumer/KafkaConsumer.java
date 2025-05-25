package ru.seeforme.notification.service.core.service.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.seeforme.notification.service.api.dto.KafkaHelpRequest;
import ru.seeforme.notification.service.core.service.NotificationService;
import ru.seeforme.notification.service.core.util.KafkaUtil;

@RequiredArgsConstructor
@Component
public class KafkaConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = KafkaUtil.HELP_REQUEST_TOPIC,
            groupId = "help-request-group1",
            containerFactory = "helpRequestListenerContainerFactory"
    )
    public void listenHelpRequestTopic(@Payload KafkaHelpRequest kafkaHelpRequest) {
        notificationService.sendHelpRequestNotification(kafkaHelpRequest);
    }
}
