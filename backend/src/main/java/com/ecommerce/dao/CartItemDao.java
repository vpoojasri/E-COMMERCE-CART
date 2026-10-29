package com.ecommerce.dao;

import com.ecommerce.entity.CartItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CartItemDao {

    @PersistenceContext
    private EntityManager em;

    public CartItem save(CartItem item) {
        if (item.getId() == null) {
            em.persist(item);
            return item;
        }
        return em.merge(item);
    }

    public Optional<CartItem> findById(Long id) {
        return Optional.ofNullable(em.find(CartItem.class, id));
    }

    public List<CartItem> findByUserId(Long userId) {
        return em.createQuery(
                        "SELECT c FROM CartItem c JOIN FETCH c.product p JOIN FETCH p.category WHERE c.user.id = :uid ORDER BY c.id ASC",
                        CartItem.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    public Optional<CartItem> findByUserAndProduct(Long userId, Long productId) {
        List<CartItem> list = em.createQuery(
                        "SELECT c FROM CartItem c WHERE c.user.id = :uid AND c.product.id = :pid",
                        CartItem.class)
                .setParameter("uid", userId)
                .setParameter("pid", productId)
                .getResultList();
        return list.stream().findFirst();
    }

    public void delete(CartItem item) {
        em.remove(em.contains(item) ? item : em.merge(item));
    }

    public void deleteAllByUserId(Long userId) {
        em.createQuery("DELETE FROM CartItem c WHERE c.user.id = :uid")
                .setParameter("uid", userId)
                .executeUpdate();
    }
}

