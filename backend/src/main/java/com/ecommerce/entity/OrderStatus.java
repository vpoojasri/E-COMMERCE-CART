package com.ecommerce.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the order lifecycle:
 * PLACED -> CONFIRMED -> PACKED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED
 * Plus CANCELLED where allowed (PLACED, CONFIRMED, PACKED).
 */
public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PACKED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PLACED, EnumSet.of(CONFIRMED, CANCELLED));
        ALLOWED_TRANSITIONS.put(CONFIRMED, EnumSet.of(PACKED, CANCELLED));
        ALLOWED_TRANSITIONS.put(PACKED, EnumSet.of(SHIPPED, CANCELLED));
        ALLOWED_TRANSITIONS.put(SHIPPED, EnumSet.of(OUT_FOR_DELIVERY));
        ALLOWED_TRANSITIONS.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED));
        ALLOWED_TRANSITIONS.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransitionTo(OrderStatus next) {
        if (next == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)).contains(next);
    }

    public boolean isCancellableByCustomer() {
        return this == PLACED || this == CONFIRMED;
    }

    public boolean isCancellableByAdmin() {
        return this == PLACED || this == CONFIRMED || this == PACKED;
    }

    public OrderStatus getNextProgressionStage() {
        return switch (this) {
            case PLACED -> CONFIRMED;
            case CONFIRMED -> PACKED;
            case PACKED -> SHIPPED;
            case SHIPPED -> OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DELIVERED;
            default -> null;
        };
    }

    public Set<OrderStatus> getAllowedNextStatuses() {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class));
    }
}

