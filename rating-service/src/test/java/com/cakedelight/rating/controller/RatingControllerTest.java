package com.cakedelight.rating.controller;

import com.cakedelight.rating.dto.RatingSummary;
import com.cakedelight.rating.service.RatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RatingController.class)
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RatingService ratingService;

    @Test
    void scoreOutOfRangeIsRejected() throws Exception {
        mockMvc.perform(post("/api/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cakeId\":1,\"score\":99,\"reviewText\":\"test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("score: score must be at most 5"));
    }

    @Test
    void averageReturnsSummary() throws Exception {
        when(ratingService.getRatingSummary(2L)).thenReturn(new RatingSummary(2L, 4.5, 2));

        mockMvc.perform(get("/api/ratings/cake/2/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(4.5))
                .andExpect(jsonPath("$.ratingCount").value(2));
    }
}
