package com.cakedelight.notification;

import com.cakedelight.notification.event.OrderCompletedEvent;
import com.cakedelight.notification.model.DeliveryStatus;
import com.cakedelight.notification.model.NotificationRecord;
import com.cakedelight.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @Autowired
    private NotificationService notificationService;

    @Test
    void duplicateEventsProduceASingleConfirmation() {
        OrderCompletedEvent event = new OrderCompletedEvent(7L, new BigDecimal("1548.00"), "COMPLETED", Instant.now());

        assertThat(notificationService.sendOrderConfirmation(event)).isTrue();
        assertThat(notificationService.sendOrderConfirmation(event)).isFalse();

        List<NotificationRecord> records = notificationService.findByOrder(7L);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(records.get(0).getMessage()).contains("#7").contains("$1548.00");
    }
}
