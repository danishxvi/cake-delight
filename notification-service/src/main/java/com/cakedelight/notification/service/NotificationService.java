package com.cakedelight.notification.service;

import com.cakedelight.notification.event.OrderCompletedEvent;
import com.cakedelight.notification.model.DeliveryStatus;
import com.cakedelight.notification.model.NotificationRecord;
import com.cakedelight.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Creates order confirmations.
 *
 * The confirmation is delivered in-app: it is stored here and shown to the
 * customer by the frontend through the REST API. An email or SMS provider
 * could be added as a further delivery channel in this class.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Records a confirmation for the order. RabbitMQ delivers at least once,
     * so an event for an order that already has a SENT confirmation is
     * ignored instead of creating a duplicate.
     *
     * @return {@code false} if the event was a duplicate
     */
    @Transactional
    public boolean sendOrderConfirmation(OrderCompletedEvent event) {
        if (notificationRepository.existsByOrderIdAndStatus(event.orderId(), DeliveryStatus.SENT)) {
            log.info("Confirmation for order {} was already sent; ignoring duplicate event", event.orderId());
            return false;
        }
        String message = "Your order #%d has been confirmed. Total amount: $%s."
                .formatted(event.orderId(), formatAmount(event.totalAmount()));
        notificationRepository.save(new NotificationRecord(event.orderId(), message, DeliveryStatus.SENT));
        log.info("Order confirmation sent for order {}", event.orderId());
        return true;
    }

    @Transactional
    public void recordFailure(Long orderId, Exception cause) {
        String message = "Failed to send order confirmation: " + cause.getMessage();
        notificationRepository.save(new NotificationRecord(orderId, truncate(message), DeliveryStatus.FAILED));
    }

    @Transactional(readOnly = true)
    public List<NotificationRecord> findByOrder(Long orderId) {
        return notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Transactional(readOnly = true)
    public List<NotificationRecord> findRecent() {
        return notificationRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private static String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String truncate(String text) {
        return text.length() <= 500 ? text : text.substring(0, 500);
    }
}
