package com.cakedelight.catalog.repository;

import com.cakedelight.catalog.model.Cake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.util.List;

public interface CakeRepository extends JpaRepository<Cake, Long>, JpaSpecificationExecutor<Cake> {

    List<Cake> findByNameContainingIgnoreCaseOrderByIdAsc(String name);

    List<Cake> findByCategoryIgnoreCaseOrderByIdAsc(String category);

    List<Cake> findByPriceBetweenOrderByIdAsc(BigDecimal minPrice, BigDecimal maxPrice);
}
