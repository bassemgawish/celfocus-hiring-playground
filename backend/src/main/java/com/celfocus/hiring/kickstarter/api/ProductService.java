package com.celfocus.hiring.kickstarter.api;

import com.celfocus.hiring.kickstarter.db.entity.ProductEntity;
import com.celfocus.hiring.kickstarter.db.repo.ProductRepository;
import com.celfocus.hiring.kickstarter.domain.Product;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductService {

    public final ConcurrentHashMap<String, AtomicInteger> stock = new ConcurrentHashMap<>();
    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<? extends Product> getProducts() {
        return productRepository.findAll();
    }

    public void loadFromProducts() {
        List<ProductEntity> products = productRepository.findAll();
        for (var p : products) {
            stock.put(p.getSku(), new AtomicInteger(10)); // need stock in json
        }
    }

    @Transactional
    public ProductEntity reserve(String sku, int qty) {
        ProductEntity product = productRepository.findBySku(sku).orElseThrow(
                () -> new RuntimeException("Cart Item not found"));
        if (stock.get(sku) == null) {
            loadFromProducts();
        }
        System.out.println("reserve sku " + sku + " qty " + qty);
        System.out.println("total stock sku before" + stock.get(sku));

        stock.computeIfPresent(sku, (k, v) -> {
            return new AtomicInteger(
                    v.updateAndGet(current -> {
                        if (current < qty) {
                            throw new EntityNotFoundException("Not enough stock");
                        }
                        return current - qty;
                    })
            );
        });
        return product;
    }

    @Transactional
    public void release(String sku, int qty) {
        System.out.println("release:total stock in sku before " + stock.get(sku));
        stock.computeIfPresent(sku, (k, v) -> {
            v.addAndGet(qty);
            System.out.println("release:total stock in sku after " + stock.get(sku));
            return v;
        });
    }

    public Optional<? extends Product> getProduct(String sku) {
        return productRepository.findBySku(sku);
    }
}
