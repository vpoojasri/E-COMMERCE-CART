package com.ecommerce.dao;

import com.ecommerce.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ProductDao {

    @PersistenceContext
    private EntityManager em;

    public Product save(Product product) {
        if (product.getId() == null) {
            em.persist(product);
            return product;
        }
        return em.merge(product);
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(em.find(Product.class, id));
    }

    public Optional<Product> findByIdForUpdate(Long id) {
        Product product = em.find(Product.class, id, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(product);
    }

    public void delete(Product product) {
        em.remove(em.contains(product) ? product : em.merge(product));
    }

    public List<Product> searchAndFilter(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean activeOnly,
            String sortBy,
            int page,
            int size) {

        StringBuilder jpql = new StringBuilder("SELECT p FROM Product p JOIN FETCH p.category WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();
        appendPredicates(jpql, params, keyword, categoryId, minPrice, maxPrice, activeOnly);

        if ("price_asc".equalsIgnoreCase(sortBy)) {
            jpql.append(" ORDER BY p.price ASC, p.id ASC");
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            jpql.append(" ORDER BY p.price DESC, p.id ASC");
        } else if ("name_asc".equalsIgnoreCase(sortBy)) {
            jpql.append(" ORDER BY LOWER(p.name) ASC, p.id ASC");
        } else if ("name_desc".equalsIgnoreCase(sortBy)) {
            jpql.append(" ORDER BY LOWER(p.name) DESC, p.id ASC");
        } else {
            jpql.append(" ORDER BY p.createdAt DESC, p.id DESC");
        }

        TypedQuery<Product> query = em.createQuery(jpql.toString(), Product.class);
        params.forEach(query::setParameter);
        query.setFirstResult(Math.max(0, page) * Math.max(1, size));
        query.setMaxResults(Math.max(1, size));
        return query.getResultList();
    }

    public long countFiltered(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean activeOnly) {

        StringBuilder jpql = new StringBuilder("SELECT COUNT(p) FROM Product p WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();
        appendPredicates(jpql, params, keyword, categoryId, minPrice, maxPrice, activeOnly);

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private void appendPredicates(
            StringBuilder jpql,
            Map<String, Object> params,
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean activeOnly) {

        if (Boolean.TRUE.equals(activeOnly)) {
            jpql.append(" AND p.active = true ");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            jpql.append(" AND (LOWER(p.name) LIKE :kw OR LOWER(p.description) LIKE :kw) ");
            params.put("kw", "%" + keyword.trim().toLowerCase() + "%");
        }
        if (categoryId != null) {
            jpql.append(" AND p.category.id = :catId ");
            params.put("catId", categoryId);
        }
        if (minPrice != null) {
            jpql.append(" AND p.price >= :minPrice ");
            params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            jpql.append(" AND p.price <= :maxPrice ");
            params.put("maxPrice", maxPrice);
        }
    }

    public long countAll() {
        return em.createQuery("SELECT COUNT(p) FROM Product p", Long.class).getSingleResult();
    }

    public List<Product> findLowStockProducts(int threshold) {
        return em.createQuery(
                        "SELECT p FROM Product p JOIN FETCH p.category WHERE p.active = true AND p.stockQuantity <= :threshold ORDER BY p.stockQuantity ASC",
                        Product.class)
                .setParameter("threshold", threshold)
                .setMaxResults(15)
                .getResultList();
    }
}

