package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.Customer;
import com.banking.OnlineBankingWeb.repository.CustomerRepository;
import com.banking.OnlineBankingWeb.service.EmailService;
import com.banking.OnlineBankingWeb.model.SecurityLog;
import com.banking.OnlineBankingWeb.repository.SecurityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.banking.OnlineBankingWeb.service.OTPService;
import org.springframework.web.bind.annotation.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired private CustomerRepository customerRepository;
    @Autowired(required = false) private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SecurityLogRepository securityLogRepository;
    @Autowired private OTPService otpService;

    private final SecureRandom secureRandom = new SecureRandom();

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
                          HttpServletRequest request, HttpSession session, Model model) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer == null) { model.addAttribute("error", "No account found with this email."); return "login"; }
        if (customer.getStatus().equals("LOCKED")) { model.addAttribute("error", "Account locked. Use Forgot Password to unlock."); return "login"; }
        if (customer.getStatus().equals("PENDING")) { model.addAttribute("error", "Account pending admin approval. Please wait."); return "login"; }
        
        if (!passwordMatches(customer, password)) {
            customer.setFailedLogins(customer.getFailedLogins() + 1);
            SecurityLog log = new SecurityLog();
            log.setCustomerId(customer.getCustomerID());
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            
            if (customer.getFailedLogins() >= 3) {
                customer.setStatus("LOCKED");
                customerRepository.save(customer);
                log.setAction("ACCOUNT_LOCKED");
                securityLogRepository.save(log);
                model.addAttribute("error", "Account locked after 3 failed attempts. Use Forgot Password.");
            } else {
                customerRepository.save(customer);
                log.setAction("LOGIN_FAILED");
                securityLogRepository.save(log);
                model.addAttribute("error", "Invalid password. " + (3 - customer.getFailedLogins()) + " attempt(s) remaining.");
            }
            return "login";
        }
        
        customer.setFailedLogins(0);
        upgradeLegacyPasswordIfNeeded(customer, password);
        customerRepository.save(customer);
        String otp = otpService.generateOtp(customer.getCustomerID());
        System.out.println("=== 2FA OTP for " + email + " : " + otp + " ===");
        boolean emailSent = false;
        if (emailService != null) {
            emailSent = emailService.sendOTP(email, otp, "2fa");
        }
        session.setAttribute("pending_user", customer);
        model.addAttribute("email", email);
        model.addAttribute("type", "2fa");
        if (!emailSent) {
            model.addAttribute("otpFallback", otp);
            model.addAttribute("success", "Email delivery unavailable. Your OTP is shown below.");
        }
        return "verify-otp";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("user");
        if (customer != null) {
            SecurityLog log = new SecurityLog();
            log.setCustomerId(customer.getCustomerID());
            log.setAction("LOGOUT");
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            securityLogRepository.save(log);
        }
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgotSubmit(@RequestParam String email, Model model) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer == null) { model.addAttribute("error", "No account found with this email."); return "forgot-password"; }
        String otp = otpService.generateOtp(customer.getCustomerID());
        System.out.println("=== RESET OTP for " + email + " : " + otp + " ===");
        boolean emailSent = false;
        if (emailService != null) {
            emailSent = emailService.sendOTP(email, otp, "reset");
        }
        model.addAttribute("email", email);
        model.addAttribute("type", "reset");
        if (emailSent) {
            model.addAttribute("success", "OTP sent to " + email + "! Check your inbox.");
        } else {
            model.addAttribute("otpFallback", otp);
            model.addAttribute("success", "Email delivery unavailable. Your OTP is shown below.");
        }
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp,
                            @RequestParam String type, HttpServletRequest request, HttpSession session, Model model) {
        Customer customer = customerRepository.findByEmail(email);
        boolean valid = otpService.verifyOtp(customer != null ? customer.getCustomerID() : -1, otp);
        if (!valid) {
            model.addAttribute("error", "Invalid or expired OTP. Please try again.");
            model.addAttribute("email", email);
            model.addAttribute("type", type);
            return "verify-otp";
        }
        if (type.equals("2fa")) {
            Customer pendingCustomer = (Customer) session.getAttribute("pending_user");
            if (pendingCustomer == null) return "redirect:/login";
            
            // Log login success
            SecurityLog log = new SecurityLog();
            log.setCustomerId(pendingCustomer.getCustomerID());
            log.setAction("LOGIN_SUCCESS");
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            securityLogRepository.save(log);

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
        Customer customer = customerRepository.findByEmail(email);
        String otp = otpService.generateOtp(customer != null ? customer.getCustomerID() : -1);
        System.out.println("=== RESENT OTP for " + email + " : " + otp + " ===");
        boolean emailSent = false;
        if (emailService != null) {
            emailSent = emailService.sendOTP(email, otp, type);
        }
        model.addAttribute("email", email);
        model.addAttribute("type", type);
        if (emailSent) {
            model.addAttribute("success", "OTP resent successfully! Check your inbox.");
        } else {
            model.addAttribute("otpFallback", otp);
            model.addAttribute("success", "Email delivery unavailable. Your OTP is shown below.");
        }
        return "verify-otp";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email, @RequestParam String token,
                                @RequestParam String password, @RequestParam String confirmPassword, 
                                HttpServletRequest request, Model model) {
        String stored = resetTokens.get(email);
        if (stored == null || !stored.equals(token)) { model.addAttribute("error", "Invalid reset link."); return "reset-password"; }
        if (!password.equals(confirmPassword)) { model.addAttribute("error", "Passwords do not match."); model.addAttribute("email", email); model.addAttribute("token", token); return "reset-password"; }
        if (password.length() < 8) { model.addAttribute("error", "Password must be at least 8 characters."); model.addAttribute("email", email); model.addAttribute("token", token); return "reset-password"; }
        Customer customer = customerRepository.findByEmail(email);
        if (customer != null) {
            customer.setPassword(passwordEncoder.encode(password)); customer.setStatus("ACTIVE"); customer.setFailedLogins(0);
            customerRepository.save(customer); resetTokens.remove(email);
            
            // Log password reset
            SecurityLog log = new SecurityLog();
            log.setCustomerId(customer.getCustomerID());
            log.setAction("PASSWORD_RESET");
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            securityLogRepository.save(log);
        }
        return "redirect:/login?success=Password+reset+successfully!+Please+login.";
    }

    private String generateOTP() { return String.format("%06d", secureRandom.nextInt(1_000_000)); }

    private boolean passwordMatches(Customer customer, String rawPassword) {
        String storedPassword = customer.getPassword();
        if (storedPassword == null) {
            return false;
        }
        if (isBCrypt(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    private void upgradeLegacyPasswordIfNeeded(Customer customer, String rawPassword) {
        if (customer.getPassword() != null && !isBCrypt(customer.getPassword())) {
            customer.setPassword(passwordEncoder.encode(rawPassword));
        }
    }

    private boolean isBCrypt(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    // ─── SMTP Email Configuration Diagnostic Endpoint ───────────
    @GetMapping("/api/test-email")
    @ResponseBody
    public String testEmail(@RequestParam(defaultValue = "fantusharyanfkb@gmail.com") String to) {
        if (emailService == null) {
            return "<h3>Error: EmailService is null!</h3> Check Spring Boot configuration.";
        }
        try {
            emailService.testEmailSend(to);
            return "<h3>Success!</h3> Test email successfully sent to " + to + ". Check your inbox/spam folders.";
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            return "<h3>Exception Occurred:</h3> <p style='color:red; font-weight:bold;'>" + e.getMessage() + "</p>"
                   + "<pre style='background:#f4f4f4; padding:15px; border-radius:8px; border:1px solid #ddd;'>" 
                   + sw.toString() + "</pre>";
        }
    }
}