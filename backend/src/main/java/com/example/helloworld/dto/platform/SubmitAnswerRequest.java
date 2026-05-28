package com.example.helloworld.dto.platform;

import java.util.List;

public record SubmitAnswerRequest(
        Long optionId,
        List<Long> optionIds
) {
}
