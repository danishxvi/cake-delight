package com.cakedelight.catalog;

import com.cakedelight.catalog.model.Cake;
import com.cakedelight.catalog.service.CakeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CakeCatalogServiceApplicationTests {

    @Autowired
    private CakeService cakeService;

    @Test
    void combinedFiltersQueryTheDatabase() {
        cakeService.addCake(new Cake("Chocolate Truffle Cake", "d", "Chocolate", new BigDecimal("799.00"), true, null));
        cakeService.addCake(new Cake("Black Forest Classic", "d", "Chocolate", new BigDecimal("749.00"), true, null));
        cakeService.addCake(new Cake("Vanilla Bean Bliss", "d", "Vanilla", new BigDecimal("649.00"), true, null));

        List<Cake> result = cakeService.search(null, "chocolate", null, new BigDecimal("780"));

        assertThat(result).extracting(Cake::getName).containsExactly("Black Forest Classic");
        assertThat(cakeService.search("bliss", null, null, null)).hasSize(1);
        assertThat(cakeService.search(null, null, null, null)).hasSize(3);
    }
}
