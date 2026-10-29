package com.ecommerce.dao;

import com.ecommerce.entity.Notification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class NotificationDao {

    @PersistenceContext
    private EntityManager em;

    public Notification save(Notification notification) {
        if (notification.getId() == null) {
            em.persist(notification);
            return notification;
        }
        return em.merge(notification);
    }

    public Optional<Notification> findById(Long id) {
        return Optional.ofNullable(em.find(Notification.class, id));
    }

    public List<Notification> findByUserId(Long userId) {
        return em.createQuery(
                        "SELECT n FROM Notification n LEFT JOIN FETCH n.order WHERE n.user.id = :uid ORDER BY n.createdAt DESC, n.id DESC",
                        Notification.class)
                .setParameter("uid", userId)
                .setMaxResults(30)
                .getResultList();
    }

    public void markAllAsReadForUser(Long userId) {
        em.createQuery("UPDATE Notification n SET n.readStatus = true WHERE n.user.id = :uid AND n.readStatus = false")
                .setParameter("uid", userId)
                .executeUpdate();
    }
}

