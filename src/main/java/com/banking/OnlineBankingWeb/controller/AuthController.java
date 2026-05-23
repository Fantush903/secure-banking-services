package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.Customer;
import com.banking.OnlineBankingWeb.repository.CustomerRepository;
import com.banking.OnlineBankingWeb.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Controller
public class AuthController {

    @Autowired private CustomerRepository customerRepository;
    @Autowired(required = false) private EmailService emailService;

    private final Map<String, String> otpStore = new HashMap<>();
    private final Map<String, String> resetTokens = new HashMap<>();

    @GetMapping("/")
    public String home() { return "redirect:/home"; }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String success, Model model) {
        if (success != null) model.addAttribute("success", success);
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email, @RequestParam String password,
                          HttpSession session, Model model) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer == null) { model.addAttribute("error", "No account found with this email."); return "login"; }
        if (customer.getStatus().equals("LOCKED")) { model.addAttribute("error", "Account locked. Use Forgot Password to unlock."); return "login"; }
        if (customer.getStatus().equals("PENDING")) { model.addAttribute("error", "Account pending admin approval. Please wait."); return "login"; }
        if (!customer.getPassword().equals(password)) {
            customer.setFailedLogins(customer.getFailedLogins() + 1);
            if (customer.getFailedLogins() >= 3) {
                customer.setStatus("LOCKED");
                customerRepository.save(customer);
                model.addAttribute("error", "Account locked after 3 failed attempts. Use Forgot Password.");
            } else {
                customerRepository.save(customer);
                model.addAttribute("error", "Invalid password. " + (3 - customer.getFailedLogins()) + " attempt(s) remaining.");
            }
            return "login";
        }
        customer.setFailedLogins(0);
        customerRepository.save(customer);
        String otp = generateOTP();
        otpStore.put(email, otp);
        System.out.println("=== 2FA OTP for " + email + " : " + otp + " ===");
        if (emailService != null) emailService.sendOTP(email, otp, "2fa");
        session.setAttribute("pending_user", customer);
        model.addAttribute("email", email);
        model.addAttribute("type", "2fa");
        return "verify-otp";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) { session.invalidate(); return "redirect:/login"; }

    @GetMapping("/forgot-password")
    public String forgotPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgotSubmit(@RequestParam String email, Model model) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer == null) { model.addAttribute("error", "No account found with this email."); return "forgot-password"; }
        String otp = generateOTP();
        otpStore.put(email, otp);
        System.out.println("=== RESET OTP for " + email + " : " + otp + " ===");
        if (emailService != null) emailService.sendOTP(email, otp, "reset");
        model.addAttribute("email", email);
        model.addAttribute("type", "reset");
        model.addAttribute("success", "OTP sent to " + email + "! Check your inbox or console.");
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp,
                            @RequestParam String type, HttpSession session, Model model) {
        String stored = otpStore.get(email);
        if (stored == null || !stored.equals(otp)) {
            model.addAttribute("error", "Invalid or expired OTP. Please try again.");
            model.addAttribute("email", email); model.addAttribute("type", type);
            return "verify-otp";
        }
        otpStore.remove(email);
        if (type.equals("2fa")) {
            Customer customer = (Customer) session.getAttribute("pending_user");
            if (customer == null) return "redirect:/login";
            session.setAttribute("user", customer);
            session.removeAttribute("pending_user");
            return customer.getRole().equals("ADMIN") ? "redirect:/admin/dashboard" : "redirect:/customer/dashboard";
        } else {
            String token = generateOTP() + generateOTP();
            resetTokens.put(email, token);
            model.addAttribute("email", email); model.addAttribute("token", token);
            return "reset-password";
        }
    }

    @GetMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, @RequestParam(defaultValue = "2fa") String type, Model model) {
        String otp = generateOTP();
        otpStore.put(email, otp);
        System.out.println("=== RESENT OTP for " + email + " : " + otp + " ===");
        if (emailService != null) emailService.sendOTP(email, otp, type);
        model.addAttribute("email", email); model.addAttribute("type", type);
        model.addAttribute("success", "OTP resent successfully!");
        return "verify-otp";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email, @RequestParam String token,
                                @RequestParam String password, @RequestParam String confirmPassword, Model model) {
        String stored = resetTokens.get(email);
        if (stored == null || !stored.equals(token)) { model.addAttribute("error", "Invalid reset link."); return "reset-password"; }
        if (!password.equals(confirmPassword)) { model.addAttribute("error", "Passwords do not match."); model.addAttribute("email", email); model.addAttribute("token", token); return "reset-password"; }
        if (password.length() < 8) { model.addAttribute("error", "Password must be at least 8 characters."); model.addAttribute("email", email); model.addAttribute("token", token); return "reset-password"; }
        Customer customer = customerRepository.findByEmail(email);
        if (customer != null) {
            customer.setPassword(password); customer.setStatus("ACTIVE"); customer.setFailedLogins(0);
            customerRepository.save(customer); resetTokens.remove(email);
        }
        return "redirect:/login?success=Password+reset+successfully!+Please+login.";
    }

    private String generateOTP() { return String.format("%06d", new Random().nextInt(999999)); }
}