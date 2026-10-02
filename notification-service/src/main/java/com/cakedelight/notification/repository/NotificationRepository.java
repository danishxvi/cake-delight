package com.cakedelight.notification.repository;

import com.cakedelight.notification.model.DeliveryStatus;
import com.cakedelight.notification.model.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationRecord, Long> {

    boolean existsByOrderIdAndStatus(Long orderId, DeliveryStatus status);

    List<NotificationRecord> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<NotificationRecord> findTop50ByOrderByCreatedAtDesc();
}
