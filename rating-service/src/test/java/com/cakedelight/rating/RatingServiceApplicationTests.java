package com.cakedelight.rating;

import com.cakedelight.rating.client.CatalogClient;
import com.cakedelight.rating.dto.RatingSummary;
import com.cakedelight.rating.dto.SubmitRatingRequest;
import com.cakedelight.rating.service.RatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class RatingServiceApplicationTests {

    @Autowired
    private RatingService ratingService;

    @MockitoBean
    private CatalogClient catalogClient;

    @Test
    void averageIsCalculatedByTheDatabase() {
        when(catalogClient.cakeExists(anyLong())).thenReturn(true);
        ratingService.submitRating(new SubmitRatingRequest(1L, 5, "Excellent"));
        ratingService.submitRating(new SubmitRatingRequest(1L, 4, null));
        ratingService.submitRating(new SubmitRatingRequest(1L, 4, "  "));

        RatingSummary summary = ratingService.getRatingSummary(1L);

        assertThat(summary.ratingCount()).isEqualTo(3);
        assertThat(summary.averageScore()).isEqualTo(4.33);
        assertThat(ratingService.getRatingsForCake(1L)).hasSize(3);
        assertThat(ratingService.getRatingSummary(2L).ratingCount()).isZero();
    }
}
