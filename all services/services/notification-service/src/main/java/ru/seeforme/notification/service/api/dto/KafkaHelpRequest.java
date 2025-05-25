package ru.seeforme.notification.service.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Jacksonized
public class KafkaHelpRequest {

    private Long requestCreatorId;

    private Instant createdAt;
}
