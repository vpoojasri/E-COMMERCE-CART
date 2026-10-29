package com.ecommerce.dao;

import com.ecommerce.entity.CustomerOrder;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.OrderTrackingHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderDao {

    @PersistenceContext
    private EntityManager em;

    public CustomerOrder save(CustomerOrder order) {
        if (order.getId() == null) {
            em.persist(order);
            return order;
        }
        return em.merge(order);
    }

    public OrderTrackingHistory saveTrackingHistory(OrderTrackingHistory history) {
        if (history.getId() == null) {
            em.persist(history);
            return history;
        }
        return em.merge(history);
    }

    public Optional<CustomerOrder> findById(Long id) {
        return Optional.ofNullable(em.find(CustomerOrder.class, id));
    }

    public Optional<CustomerOrder> findByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null) return Optional.empty();
        List<CustomerOrder> list = em.createQuery(
                        "SELECT o FROM CustomerOrder o JOIN FETCH o.user WHERE UPPER(o.trackingNumber) = UPPER(:tn)",
                        CustomerOrder.class)
                .setParameter("tn", trackingNumber.trim())
                .getResultList();
        return list.stream().findFirst();
    }

    public Optional<CustomerOrder> findByOrderNumber(String orderNumber) {
        if (orderNumber == null) return Optional.empty();
        List<CustomerOrder> list = em.createQuery(
                        "SELECT o FROM CustomerOrder o JOIN FETCH o.user WHERE UPPER(o.orderNumber) = UPPER(:on)",
                        CustomerOrder.class)
                .setParameter("on", orderNumber.trim())
                .getResultList();
        return list.stream().findFirst();
    }

    public List<CustomerOrder> findByUserId(Long userId, int page, int size) {
        return em.createQuery(
                        "SELECT o FROM CustomerOrder o JOIN FETCH o.user WHERE o.user.id = :uid ORDER BY o.orderDate DESC, o.id DESC",
                        CustomerOrder.class)
                .setParameter("uid", userId)
                .setFirstResult(Math.max(0, page) * Math.max(1, size))
                .setMaxResults(Math.max(1, size))
                .getResultList();
    }

    public long countByUserId(Long userId) {
        return em.createQuery("SELECT COUNT(o) FROM CustomerOrder o WHERE o.user.id = :uid", Long.class)
                .setParameter("uid", userId)
                .getSingleResult();
    }

    public List<CustomerOrder> findAllFiltered(OrderStatus statusFilter, int page, int size) {
        if (statusFilter != null) {
            return em.createQuery(
                            "SELECT o FROM CustomerOrder o JOIN FETCH o.user WHERE o.currentStatus = :st ORDER BY o.orderDate DESC, o.id DESC",
                            CustomerOrder.class)
                    .setParameter("st", statusFilter)
                    .setFirstResult(Math.max(0, page) * Math.max(1, size))
                    .setMaxResults(Math.max(1, size))
                    .getResultList();
        }
        return em.createQuery(
                        "SELECT o FROM CustomerOrder o JOIN FETCH o.user ORDER BY o.orderDate DESC, o.id DESC",
                        CustomerOrder.class)
                .setFirstResult(Math.max(0, page) * Math.max(1, size))
                .setMaxResults(Math.max(1, size))
                .getResultList();
    }

    public long countAllFiltered(OrderStatus statusFilter) {
        if (statusFilter != null) {
            return em.createQuery("SELECT COUNT(o) FROM CustomerOrder o WHERE o.currentStatus = :st", Long.class)
                    .setParameter("st", statusFilter)
                    .getSingleResult();
        }
        return em.createQuery("SELECT COUNT(o) FROM CustomerOrder o", Long.class)
                .getSingleResult();
    }

    public List<CustomerOrder> findActiveOrdersForAutomation() {
        return em.createQuery(
                        "SELECT o FROM CustomerOrder o JOIN FETCH o.user WHERE o.currentStatus IN (:s1, :s2, :s3, :s4, :s5) ORDER BY o.orderDate ASC",
                        CustomerOrder.class)
                .setParameter("s1", OrderStatus.PLACED)
                .setParameter("s2", OrderStatus.CONFIRMED)
                .setParameter("s3", OrderStatus.PACKED)
                .setParameter("s4", OrderStatus.SHIPPED)
                .setParameter("s5", OrderStatus.OUT_FOR_DELIVERY)
                .getResultList();
    }

    public List<OrderTrackingHistory> findTrackingHistoryByOrderId(Long orderId) {
        return em.createQuery(
                        "SELECT h FROM OrderTrackingHistory h WHERE h.order.id = :oid ORDER BY h.updatedAt ASC, h.id ASC",
                        OrderTrackingHistory.class)
                .setParameter("oid", orderId)
                .getResultList();
    }

    public long countByStatuses(List<OrderStatus> statuses) {
        return em.createQuery("SELECT COUNT(o) FROM CustomerOrder o WHERE o.currentStatus IN :statuses", Long.class)
                .setParameter("statuses", statuses)
                .getSingleResult();
    }

    public BigDecimal calculateTotalActiveRevenue() {
        BigDecimal sum = em.createQuery(
                        "SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.currentStatus <> :cancelled",
                        BigDecimal.class)
                .setParameter("cancelled", OrderStatus.CANCELLED)
                .getSingleResult();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public long countOrderItemsByProductId(Long productId) {
        return em.createQuery("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.product.id = :pid", Long.class)
                .setParameter("pid", productId)
                .getSingleResult();
    }
}

