package com.cakedelight.rating.dto;

/**
 * Average score and number of ratings for one cake. averageScore is 0.0 when
 * ratingCount is 0.
 */
public record RatingSummary(Long cakeId, double averageScore, long ratingCount) {
}
