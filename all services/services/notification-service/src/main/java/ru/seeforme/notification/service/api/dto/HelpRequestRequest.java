package ru.seeforme.notification.service.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class HelpRequestRequest {

    private Long requestCreatorId;
}
