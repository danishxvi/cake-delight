package com.cakedelight.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A cake in the catalog. Only a filename is stored for the image; no binary
 * image data lives in the database.
 */
@Entity
@Table(name = "cake")
public class Cake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name must be at most 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 255, message = "description must be at most 255 characters")
    private String description;

    @NotBlank(message = "category must not be blank")
    @Size(max = 50, message = "category must be at most 50 characters")
    @Column(nullable = false, length = 50)
    private String category;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.00", message = "price must be zero or positive")
    @Digits(integer = 8, fraction = 2, message = "price must have at most 2 decimal places")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "availability is required")
    @Column(nullable = false)
    private Boolean availability = Boolean.TRUE;

    @Size(max = 255, message = "imageReference must be at most 255 characters")
    private String imageReference;

    public Cake() {
    }

    public Cake(String name, String description, String category, BigDecimal price,
                Boolean availability, String imageReference) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.availability = availability != null ? availability : Boolean.TRUE;
        this.imageReference = imageReference;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getAvailability() { return availability; }
    public void setAvailability(Boolean availability) { this.availability = availability; }

    public String getImageReference() { return imageReference; }
    public void setImageReference(String imageReference) { this.imageReference = imageReference; }
}
