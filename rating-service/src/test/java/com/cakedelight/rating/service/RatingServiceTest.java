package com.cakedelight.rating.service;

import com.cakedelight.rating.client.CatalogClient;
import com.cakedelight.rating.dto.RatingSummary;
import com.cakedelight.rating.dto.SubmitRatingRequest;
import com.cakedelight.rating.exception.ServiceUnavailableException;
import com.cakedelight.rating.model.Rating;
import com.cakedelight.rating.repository.RatingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private RatingService ratingService;

    @Test
    void submitRatingSavesWhenCakeExists() {
        when(catalogClient.cakeExists(1L)).thenReturn(true);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));

        Rating saved = ratingService.submitRating(new SubmitRatingRequest(1L, 5, "  Lovely  "));

        assertThat(saved.getScore()).isEqualTo(5);
        assertThat(saved.getReviewText()).isEqualTo("Lovely");
    }

    @Test
    void submitRatingRejectsUnknownCake() {
        when(catalogClient.cakeExists(999L)).thenReturn(false);

        assertThatThrownBy(() -> ratingService.submitRating(new SubmitRatingRequest(999L, 5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void submitRatingReportsCatalogOutage() {
        when(catalogClient.cakeExists(1L)).thenThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> ratingService.submitRating(new SubmitRatingRequest(1L, 5, null)))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void summaryForUnratedCakeIsZero() {
        when(ratingRepository.countByCakeId(3L)).thenReturn(0L);

        RatingSummary summary = ratingService.getRatingSummary(3L);

        assertThat(summary.averageScore()).isZero();
        assertThat(summary.ratingCount()).isZero();
    }

    @Test
    void summaryRoundsToTwoDecimals() {
        when(ratingRepository.countByCakeId(1L)).thenReturn(3L);
        when(ratingRepository.averageScoreForCake(1L)).thenReturn(13.0 / 3.0);

        assertThat(ratingService.getRatingSummary(1L).averageScore()).isEqualTo(4.33);
    }
}
