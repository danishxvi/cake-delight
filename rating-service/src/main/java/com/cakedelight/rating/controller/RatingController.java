package com.cakedelight.rating.controller;

import com.cakedelight.rating.dto.RatingSummary;
import com.cakedelight.rating.dto.SubmitRatingRequest;
import com.cakedelight.rating.model.Rating;
import com.cakedelight.rating.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rating REST API, reached by clients through the API Gateway at /api/ratings.
 */
@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Rating submitRating(@Valid @RequestBody SubmitRatingRequest request) {
        return ratingService.submitRating(request);
    }

    @GetMapping("/cake/{cakeId}")
    public List<Rating> getRatingsByCake(@PathVariable Long cakeId) {
        return ratingService.getRatingsForCake(cakeId);
    }

    @GetMapping("/cake/{cakeId}/average")
    public RatingSummary getAverageRating(@PathVariable Long cakeId) {
        return ratingService.getRatingSummary(cakeId);
    }
}
