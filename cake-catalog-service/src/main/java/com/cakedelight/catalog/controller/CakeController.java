package com.cakedelight.catalog.controller;

import com.cakedelight.catalog.model.Cake;
import com.cakedelight.catalog.service.CakeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * Catalog REST API, reached by clients through the API Gateway at /api/catalog.
 */
@RestController
@RequestMapping("/api/catalog")
public class CakeController {

    private final CakeService cakeService;

    public CakeController(CakeService cakeService) {
        this.cakeService = cakeService;
    }

    /**
     * Lists cakes. All filters are optional and can be combined, for example
     * {@code /api/catalog?category=Chocolate&maxPrice=780}.
     */
    @GetMapping
    public List<Cake> getCakes(@RequestParam(required = false) String name,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) BigDecimal minPrice,
                               @RequestParam(required = false) BigDecimal maxPrice) {
        return cakeService.search(name, category, minPrice, maxPrice);
    }

    @GetMapping("/{id}")
    public Cake getCakeById(@PathVariable Long id) {
        return cakeService.getCakeById(id);
    }

    @GetMapping("/search")
    public List<Cake> searchCakesByName(@RequestParam String name) {
        return cakeService.filterByName(name);
    }

    @GetMapping("/category/{category}")
    public List<Cake> getCakesByCategory(@PathVariable String category) {
        return cakeService.filterByCategory(category);
    }

    @GetMapping("/price")
    public List<Cake> getCakesByPriceRange(@RequestParam BigDecimal min, @RequestParam BigDecimal max) {
        return cakeService.filterByPrice(min, max);
    }

    @PostMapping
    public ResponseEntity<Cake> createCake(@Valid @RequestBody Cake cake) {
        Cake created = cakeService.addCake(cake);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }
}
