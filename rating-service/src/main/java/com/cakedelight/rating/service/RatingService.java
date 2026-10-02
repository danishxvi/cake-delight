package com.cakedelight.rating.service;

import com.cakedelight.rating.client.CatalogClient;
import com.cakedelight.rating.dto.RatingSummary;
import com.cakedelight.rating.dto.SubmitRatingRequest;
import com.cakedelight.rating.exception.ServiceUnavailableException;
import com.cakedelight.rating.model.Rating;
import com.cakedelight.rating.repository.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final CatalogClient catalogClient;

    public RatingService(RatingRepository ratingRepository, CatalogClient catalogClient) {
        this.ratingRepository = ratingRepository;
        this.catalogClient = catalogClient;
    }

    @Transactional
    public Rating submitRating(SubmitRatingRequest request) {
        if (!cakeExists(request.cakeId())) {
            throw new IllegalArgumentException("Cake with id " + request.cakeId() + " does not exist");
        }
        String review = request.reviewText() == null || request.reviewText().isBlank()
                ? null : request.reviewText().trim();
        return ratingRepository.save(new Rating(request.cakeId(), request.score(), review));
    }

    @Transactional(readOnly = true)
    public List<Rating> getRatingsForCake(Long cakeId) {
        return ratingRepository.findByCakeIdOrderByCreatedAtDesc(cakeId);
    }

    @Transactional(readOnly = true)
    public RatingSummary getRatingSummary(Long cakeId) {
        long count = ratingRepository.countByCakeId(cakeId);
        if (count == 0) {
            return new RatingSummary(cakeId, 0.0, 0);
        }
        double average = ratingRepository.averageScoreForCake(cakeId);
        return new RatingSummary(cakeId, Math.round(average * 100.0) / 100.0, count);
    }

    private boolean cakeExists(Long cakeId) {
        try {
            return catalogClient.cakeExists(cakeId);
        } catch (RestClientException | IllegalStateException e) {
            throw new ServiceUnavailableException(
                    "The catalog service is unavailable right now. Please try again shortly.", e);
        }
    }
}
