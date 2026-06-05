package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.Customer;
import com.banking.OnlineBankingWeb.model.Notification;
import com.banking.OnlineBankingWeb.repository.NotificationRepository;
import com.banking.OnlineBankingWeb.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/customer")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    // ── API for Current User Info and Unread Count ───────────
    @GetMapping("/notifications/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCountApi(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        if (session.getAttribute("user") == null) {
            response.put("error", "Unauthorized");
            return response;
        }
        Customer user = (Customer) session.getAttribute("user");
        response.put("customerId", user.getCustomerID());
        response.put("unreadCount", notificationService.getUnreadCount((long) user.getCustomerID()));
        return response;
    }

    // ── List Notifications ───────────────────────────────────
    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        if (session.getAttribute("user") == null) return "redirect:/login";
        Customer user = (Customer) session.getAttribute("user");

        List<Notification> notifications = notificationService.getNotifications((long) user.getCustomerID());
        long unreadCount = notificationService.getUnreadCount((long) user.getCustomerID());

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        return "notifications";
    }

    // ── Mark Single as Read ──────────────────────────────────
    @PostMapping("/notifications/read/{id}")
    public String markAsRead(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("user") == null) return "redirect:/login";
        notificationService.markAsRead(id);
        return "redirect:/customer/notifications";
    }

    // ── Mark All as Read ─────────────────────────────────────
    @PostMapping("/notifications/read-all")
    public String markAllRead(HttpSession session) {
        if (session.getAttribute("user") == null) return "redirect:/login";
        Customer user = (Customer) session.getAttribute("user");
        notificationService.markAllRead((long) user.getCustomerID());
        return "redirect:/customer/notifications";
    }
}
