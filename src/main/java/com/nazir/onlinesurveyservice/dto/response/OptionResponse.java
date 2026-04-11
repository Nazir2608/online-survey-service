package com.nazir.onlinesurveyservice.dto.response;

import java.util.UUID;

public record OptionResponse(
        UUID   id,
        String text,
        int    orderIndex
) {}
