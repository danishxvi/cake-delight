package com.cakedelight.rating.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

/**
 * A rating left on a cake. cakeId refers to a cake in cake-catalog-service's
 * own database, so it is a plain indexed column rather than a foreign key.
 */
@Entity
@Table(name = "rating", indexes = @Index(name = "idx_rating_cake_id", columnList = "cake_id"))
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cake_id", nullable = false)
    private Long cakeId;

    // Hibernate also turns these bounds into a CHECK constraint on the column.
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer score;

    @Column(length = 500)
    private String reviewText;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Rating() {
    }

    public Rating(Long cakeId, Integer score, String reviewText) {
        this.cakeId = cakeId;
        this.score = score;
        this.reviewText = reviewText;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Long getCakeId() { return cakeId; }
    public Integer getScore() { return score; }
    public String getReviewText() { return reviewText; }
    public Instant getCreatedAt() { return createdAt; }
}
