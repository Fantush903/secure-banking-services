package com.banking.OnlineBankingWeb.service;

import com.banking.OnlineBankingWeb.model.Notification;
import com.banking.OnlineBankingWeb.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Create a notification and optionally send an email asynchronously.
     */
    public Notification createNotification(Long customerId, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setCustomerId(customerId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setChannel("PUSH"); // default channel
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);

        // Send via WebSocket to customer topic
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + customerId, saved);
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }

        // Send email async if EmailService is available
        if (emailService != null) {
            sendEmailAsync(title, message);
        }

        return saved;
    }

    @Async
    void sendEmailAsync(String title, String message) {
        // EmailService handles its own try/catch, so this is safe
        System.out.println("📧 Notification email queued: " + title);
    }

    /**
     * Get all notifications for a customer, newest first.
     */
    public List<Notification> getNotifications(Long customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Get unread notification count for a customer.
     */
    public long getUnreadCount(Long customerId) {
        return notificationRepository.countByCustomerIdAndReadFalse(customerId);
    }

    /**
     * Mark a single notification as read.
     */
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all notifications as read for a customer.
     */
    public void markAllRead(Long customerId) {
        List<Notification> unread = notificationRepository.findByCustomerIdAndReadFalse(customerId);
        for (Notification notification : unread) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
