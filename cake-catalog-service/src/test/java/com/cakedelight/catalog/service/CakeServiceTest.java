package com.cakedelight.catalog.service;

import com.cakedelight.catalog.exception.ResourceNotFoundException;
import com.cakedelight.catalog.model.Cake;
import com.cakedelight.catalog.repository.CakeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CakeServiceTest {

    @Mock
    private CakeRepository cakeRepository;

    @InjectMocks
    private CakeService cakeService;

    @Test
    void getCakeByIdThrowsWhenMissing() {
        when(cakeRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cakeService.getCakeById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void addCakeIgnoresClientSuppliedId() {
        Cake cake = new Cake("Test Cake", "desc", "Test", new BigDecimal("10.00"), true, null);
        cake.setId(1L);
        when(cakeRepository.save(any(Cake.class))).thenAnswer(inv -> inv.getArgument(0));

        Cake saved = cakeService.addCake(cake);

        assertThat(saved.getId()).isNull();
        verify(cakeRepository).save(cake);
    }

    @Test
    void priceFilterRejectsInvertedRange() {
        assertThatThrownBy(() -> cakeService.filterByPrice(new BigDecimal("500"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void priceFilterRejectsNegativeMinimum() {
        assertThatThrownBy(() -> cakeService.search(null, null, new BigDecimal("-1"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
