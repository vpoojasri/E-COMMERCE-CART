package com.ecommerce.scheduler;

import com.ecommerce.service.OrderTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Simulated Automated Order Progression Engine for College Lab Demonstration.
 * Disabled by default in normal use as required by the specification.
 * Can be enabled via environment variable TRACKING_AUTOMATION_ENABLED=true
 * or toggled live from the Administrator Dashboard.
 */
@Component
public class OrderTrackingAutomationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTrackingAutomationScheduler.class);

    private final OrderTrackingService orderTrackingService;

    public OrderTrackingAutomationScheduler(OrderTrackingService orderTrackingService) {
        this.orderTrackingService = orderTrackingService;
    }

    @Scheduled(fixedDelayString = "${tracking.automation.interval.ms:25000}", initialDelay = 15000)
    public void runAutomatedTrackingProgression() {
        if (!orderTrackingService.isAutomationEnabled()) {
            return;
        }
        try {
            int updated = orderTrackingService.advanceActiveOrdersInSimulationStep("Automated Tracking Engine (Demo)");
            if (updated > 0) {
                log.info("Automated Tracking Scheduler advanced {} active order(s) to their next delivery stage.", updated);
            }
        } catch (Exception e) {
            log.warn("Automated Tracking Scheduler encountered an issue: {}", e.getMessage());
        }
    }
}

