package io.synub.billing.web;

import io.synub.billing.dto.Dtos.ProductDto;
import io.synub.billing.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return catalog.listProducts();
    }
}
