package com.cakedelight.catalog.controller;

import com.cakedelight.catalog.exception.ResourceNotFoundException;
import com.cakedelight.catalog.model.Cake;
import com.cakedelight.catalog.service.CakeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CakeController.class)
class CakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CakeService cakeService;

    @Test
    void listsCakesWithCombinedFilters() throws Exception {
        Cake cake = new Cake("Black Forest Classic", "d", "Chocolate", new BigDecimal("749.00"), true, null);
        when(cakeService.search("forest", "Chocolate", null, new BigDecimal("800"))).thenReturn(List.of(cake));

        mockMvc.perform(get("/api/catalog").param("name", "forest").param("category", "Chocolate").param("maxPrice", "800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Black Forest Classic"));
    }

    @Test
    void returns404ForUnknownCake() throws Exception {
        when(cakeService.getCakeById(99L)).thenThrow(new ResourceNotFoundException("Cake with id 99 was not found"));

        mockMvc.perform(get("/api/catalog/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cake with id 99 was not found"))
                .andExpect(jsonPath("$.path").value("/api/catalog/99"));
    }

    @Test
    void returns400ForMissingQueryParameter() throws Exception {
        mockMvc.perform(get("/api/catalog/price").param("min", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returns400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/catalog/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForUnknownPath() throws Exception {
        mockMvc.perform(get("/api/catalog/foo/bar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidCake() throws Exception {
        mockMvc.perform(post("/api/catalog").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"category\":\"Test\",\"price\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsCakeWithLocationHeader() throws Exception {
        Cake saved = new Cake("New Cake", "d", "Test", new BigDecimal("100.00"), true, null);
        saved.setId(9L);
        when(cakeService.addCake(any(Cake.class))).thenReturn(saved);

        mockMvc.perform(post("/api/catalog").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Cake\",\"category\":\"Test\",\"price\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/catalog/9"));
    }
}
