package com.cakedelight.rating.repository;

import com.cakedelight.rating.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByCakeIdOrderByCreatedAtDesc(Long cakeId);

    long countByCakeId(Long cakeId);

    // Computed by the database instead of loading every rating into memory.
    @Query("select avg(r.score) from Rating r where r.cakeId = :cakeId")
    Double averageScoreForCake(@Param("cakeId") Long cakeId);
}
