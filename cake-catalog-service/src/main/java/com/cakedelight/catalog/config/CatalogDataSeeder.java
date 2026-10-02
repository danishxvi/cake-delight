package com.cakedelight.catalog.config;

import com.cakedelight.catalog.model.Cake;
import com.cakedelight.catalog.repository.CakeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the catalog with starter cakes so a fresh deployment is usable
 * immediately. Runs only when the table is empty, so restarts never create
 * duplicates. Disabled under the "test" profile.
 */
@Component
@Profile("!test")
public class CatalogDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataSeeder.class);

    private final CakeRepository cakeRepository;

    public CatalogDataSeeder(CakeRepository cakeRepository) {
        this.cakeRepository = cakeRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cakeRepository.count() > 0) {
            return;
        }
        List<Cake> cakes = List.of(
                cake("Chocolate Truffle Cake", "Rich dark chocolate sponge layered with silky truffle ganache", "Chocolate", "799.00", true, "chocolate-truffle.jpg"),
                cake("Red Velvet Delight", "Classic red velvet with cream cheese frosting", "Signature", "849.00", true, "red-velvet.jpg"),
                cake("Vanilla Bean Bliss", "Light vanilla sponge with Madagascar bean-flecked buttercream", "Vanilla", "649.00", true, "vanilla-bean.jpg"),
                cake("Black Forest Classic", "Chocolate sponge, whipped cream, and cherries", "Chocolate", "749.00", true, "black-forest.jpg"),
                cake("Butterscotch Crunch", "Butterscotch sponge with caramelized nut crunch", "Butterscotch", "699.00", true, "butterscotch.jpg"),
                cake("Strawberry Shortcake", "Fresh strawberries layered with vanilla cream", "Fruit", "729.00", true, "strawberry.jpg"),
                cake("Pineapple Paradise", "Light sponge with fresh pineapple and whipped cream", "Fruit", "599.00", true, "pineapple.jpg"),
                cake("Coffee Walnut Cake", "Coffee-infused sponge with walnut buttercream", "Coffee", "799.00", false, "coffee-walnut.jpg"));
        cakeRepository.saveAll(cakes);
        log.info("Seeded catalog with {} cakes", cakes.size());
    }

    private static Cake cake(String name, String description, String category, String price,
                             boolean available, String imageReference) {
        return new Cake(name, description, category, new BigDecimal(price), available, imageReference);
    }
}
