package com.parlament.service;

import com.parlament.model.Category;
import com.parlament.model.Product;
import com.parlament.repository.CategoryRepository;
import com.parlament.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;

    @Cacheable("categories")
    public List<Category> getActiveCategories() {
        return categoryRepo.findByActiveTrueOrderBySortOrderAsc();
    }

    @Cacheable("products")
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepo.findByCategoryIdAndAvailableTrueOrderBySortOrderAsc(categoryId);
    }

    public Optional<Product> findProduct(Long id) {
        return productRepo.findById(id);
    }

    public Optional<Category> findCategory(Long id) {
        return categoryRepo.findById(id);
    }

    @Transactional
    @CacheEvict(value = {"categories", "products"}, allEntries = true)
    public Product saveProduct(Product product) {
        return productRepo.save(product);
    }

    @Transactional
    @CacheEvict(value = {"categories", "products"}, allEntries = true)
    public Category saveCategory(Category category) {
        return categoryRepo.save(category);
    }

    @Transactional
    @CacheEvict(value = {"categories", "products"}, allEntries = true)
    public void toggleProductAvailability(Long productId) {
        productRepo.findById(productId).ifPresent(p -> {
            p.setAvailable(!p.isAvailable());
            productRepo.save(p);
        });
    }

    public long countProducts() { return productRepo.countByAvailableTrue(); }
    public long countCategories() { return categoryRepo.count(); }
}
