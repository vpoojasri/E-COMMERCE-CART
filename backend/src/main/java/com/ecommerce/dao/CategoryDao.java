package com.ecommerce.dao;

import com.ecommerce.entity.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryDao {

    @PersistenceContext
    private EntityManager em;

    public Category save(Category category) {
        if (category.getId() == null) {
            em.persist(category);
            return category;
        }
        return em.merge(category);
    }

    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(em.find(Category.class, id));
    }

    public Optional<Category> findByName(String name) {
        List<Category> list = em.createQuery("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)", Category.class)
                .setParameter("name", name.trim())
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
        return list.stream().findFirst();
    }

    public List<Category> findAllCached() {
        return em.createQuery("SELECT c FROM Category c ORDER BY c.name ASC", Category.class)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", "categoriesQueryCache")
                .getResultList();
    }

    public void delete(Category category) {
        em.remove(em.contains(category) ? category : em.merge(category));
    }

    public long countProductsInCategory(Long categoryId) {
        return em.createQuery("SELECT COUNT(p) FROM Product p WHERE p.category.id = :catId", Long.class)
                .setParameter("catId", categoryId)
                .getSingleResult();
    }
}

