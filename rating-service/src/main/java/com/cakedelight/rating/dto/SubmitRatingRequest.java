package com.cakedelight.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitRatingRequest(
        @NotNull(message = "cakeId is required")
        Long cakeId,

        @NotNull(message = "score is required")
        @Min(value = 1, message = "score must be at least 1")
        @Max(value = 5, message = "score must be at most 5")
        Integer score,

        @Size(max = 500, message = "reviewText must be at most 500 characters")
        String reviewText) {
}
