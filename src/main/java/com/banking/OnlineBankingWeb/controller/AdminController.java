package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.Customer;
import com.banking.OnlineBankingWeb.repository.CustomerRepository;
import com.banking.OnlineBankingWeb.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private CustomerRepository customerRepository;
    @Autowired(required = false) private EmailService emailService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        model.addAttribute("customers", customerRepository.findAll());
        return "admin-dashboard";
    }

    @GetMapping("/customers")
    public String customers(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        model.addAttribute("customers", customerRepository.findAll());
        return "admin-customers";
    }

    @GetMapping("/transactions")
    public String transactions(HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        return "admin-transactions";
    }

    @GetMapping("/loans")
    public String loans(HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        return "admin-loans";
    }

    @GetMapping("/audit")
    public String audit(HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        return "admin-audit";
    }

    @GetMapping("/settings")
    public String settings(HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        return "admin-settings";
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        model.addAttribute("totalCustomers", customerRepository.count());
        return "admin-reports";
    }

    @GetMapping("/approvals")
    public String approvals(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        List<Customer> all = customerRepository.findAll();
        List<Customer> pending = all.stream()
                .filter(c -> "PENDING".equals(c.getStatus()))
                .collect(Collectors.toList());
        model.addAttribute("pendingCustomers", pending);
        model.addAttribute("pendingCount", pending.size());
        return "admin-approvals";
    }

    @GetMapping("/approve/{id}")
    public String approveCustomer(@PathVariable int id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        customerRepository.findById(id).ifPresent(c -> {
            c.setStatus("ACTIVE");
            customerRepository.save(c);
            if (emailService != null) emailService.sendWelcome(c.getEmail(), c.getName());
        });
        return "redirect:/admin/approvals";
    }

    @GetMapping("/reject/{id}")
    public String rejectCustomer(@PathVariable int id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        customerRepository.findById(id).ifPresent(c -> {
            c.setStatus("INACTIVE");
            customerRepository.save(c);
        });
        return "redirect:/admin/approvals";
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable int id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        customerRepository.findById(id).ifPresent(c -> {
            c.setStatus(c.getStatus().equals("ACTIVE") ? "LOCKED" : "ACTIVE");
            c.setFailedLogins(0);
            customerRepository.save(c);
        });
        return "redirect:/admin/customers";
    }

    @GetMapping("/reset-user/{id}")
    public String resetUser(@PathVariable int id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        customerRepository.findById(id).ifPresent(c -> {
            c.setFailedLogins(0);
            c.setStatus("ACTIVE");
            customerRepository.save(c);
        });
        return "redirect:/admin/customers";
    }

    private boolean isAdmin(HttpSession session) {
        Customer user = (Customer) session.getAttribute("user");
        return user != null && "ADMIN".equals(user.getRole());
    }
}