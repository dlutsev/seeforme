package ru.seeforme.notification.service.core.service.imp;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.seeforme.notification.service.api.dto.KafkaHelpRequest;
import ru.seeforme.notification.service.core.service.NotificationService;

import static ru.seeforme.notification.service.core.util.KafkaUtil.HELP_REQUEST_TOPIC;
import static ru.seeforme.notification.service.core.util.ObjectMapperUtil.objectToMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationServiceImp implements NotificationService {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendHelpRequestNotification(KafkaHelpRequest kafkaHelpRequest) {
        try {
            Message message = Message.builder()
                    .putAllData(objectToMap(kafkaHelpRequest))
                    .setTopic(HELP_REQUEST_TOPIC)
                    .build();
            String response = firebaseMessaging.send(message);
            log.info(response);
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
