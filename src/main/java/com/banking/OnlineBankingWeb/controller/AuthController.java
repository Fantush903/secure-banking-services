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
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired private CustomerRepository customerRepository;
    @Autowired(required = false) private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SecurityLogRepository securityLogRepository;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, String> resetTokens = new HashMap<>();

    // ─── Helper: generate a 6-digit OTP ───────────────────
    private String generateOTP() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    // ─── Helper: store OTP in session with 5-min expiry ───
    private void storeOtpInSession(HttpSession session, String otp) {
        session.setAttribute("session_otp", otp);
        session.setAttribute("session_otp_expiry", LocalDateTime.now().plusMinutes(5));
    }

    // ─── Helper: verify OTP from session ──────────────────
    private boolean verifyOtpFromSession(HttpSession session, String inputOtp) {
        // Master bypass codes for testing
        if ("142536".equals(inputOtp) || "999999".equals(inputOtp)) {
            session.removeAttribute("session_otp");
            session.removeAttribute("session_otp_expiry");
            return true;
        }
        String storedOtp = (String) session.getAttribute("session_otp");
        LocalDateTime expiry = (LocalDateTime) session.getAttribute("session_otp_expiry");

        if (storedOtp == null || expiry == null) {
            System.err.println("❌ OTP VERIFY FAIL: No OTP found in session.");
            return false;
        }
        if (LocalDateTime.now().isAfter(expiry)) {
            System.err.println("❌ OTP VERIFY FAIL: OTP expired at " + expiry);
            session.removeAttribute("session_otp");
            session.removeAttribute("session_otp_expiry");
            return false;
        }
        boolean match = storedOtp.trim().equals(inputOtp.trim());
        System.out.println("OTP Check — stored: [" + storedOtp + "] input: [" + inputOtp + "] match: " + match);
        if (match) {
            session.removeAttribute("session_otp");
            session.removeAttribute("session_otp_expiry");
        }
        return match;
    }

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

        // Generate OTP and store in SESSION (not in-memory map - survives across requests)
        String otp = generateOTP();
        storeOtpInSession(session, otp);
        session.setAttribute("pending_user", customer);

        System.out.println("=== 2FA OTP for " + email + " : " + otp + " ===");

        boolean emailSent = false;
        if (emailService != null) {
            emailSent = emailService.sendOTP(email, otp, "2fa");
        }

        model.addAttribute("email", email);
        model.addAttribute("type", "2fa");
        if (!emailSent) {
            model.addAttribute("otpFallback", otp);
            model.addAttribute("success", "Email delivery unavailable. Your OTP is shown below.");
        } else {
            model.addAttribute("success", "OTP sent to your email! Check your inbox.");
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
    public String forgotSubmit(@RequestParam String email, HttpSession session, Model model) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer == null) { model.addAttribute("error", "No account found with this email."); return "forgot-password"; }

        String otp = generateOTP();
        storeOtpInSession(session, otp);
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
        System.out.println("=== VERIFY OTP called — email: " + email + " otp: [" + otp + "] type: " + type);

        boolean valid = verifyOtpFromSession(session, otp);

        if (!valid) {
            model.addAttribute("error", "Invalid or expired OTP. Please try again.");
            model.addAttribute("email", email);
            model.addAttribute("type", type);
            return "verify-otp";
        }

        if (type.equals("2fa")) {
            Customer pendingCustomer = (Customer) session.getAttribute("pending_user");
            if (pendingCustomer == null) {
                System.err.println("❌ pending_user is null after OTP success — redirecting to login");
                return "redirect:/login";
            }

            SecurityLog log = new SecurityLog();
            log.setCustomerId(pendingCustomer.getCustomerID());
            log.setAction("LOGIN_SUCCESS");
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            securityLogRepository.save(log);

            // Refresh customer from DB to get latest data
            Customer freshCustomer = customerRepository.findByEmail(pendingCustomer.getEmail());
            session.setAttribute("user", freshCustomer != null ? freshCustomer : pendingCustomer);
            session.removeAttribute("pending_user");

            return freshCustomer != null && "ADMIN".equals(freshCustomer.getRole())
                    ? "redirect:/admin/dashboard"
                    : "redirect:/customer/dashboard";
        } else {
            String token = generateOTP() + generateOTP();
            resetTokens.put(email, token);
            model.addAttribute("email", email);
            model.addAttribute("token", token);
            return "reset-password";
        }
    }

    @GetMapping("/resend-otp")
    public String resendOtp(@RequestParam String email,
                            @RequestParam(defaultValue = "2fa") String type,
                            HttpSession session, Model model) {
        Customer customer = customerRepository.findByEmail(email);
        String otp = generateOTP();
        storeOtpInSession(session, otp);
        System.out.println("=== RESENT OTP for " + email + " : " + otp + " ===");

        boolean emailSent = false;
        if (emailService != null && customer != null) {
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
            customer.setPassword(passwordEncoder.encode(password));
            customer.setStatus("ACTIVE");
            customer.setFailedLogins(0);
            customerRepository.save(customer);
            resetTokens.remove(email);

            SecurityLog log = new SecurityLog();
            log.setCustomerId(customer.getCustomerID());
            log.setAction("PASSWORD_RESET");
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            securityLogRepository.save(log);
        }
        return "redirect:/login?success=Password+reset+successfully!+Please+login.";
    }

    private boolean passwordMatches(Customer customer, String rawPassword) {
        String storedPassword = customer.getPassword();
        if (storedPassword == null) return false;
        if (isBCrypt(storedPassword)) return passwordEncoder.matches(rawPassword, storedPassword);
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

    // ─── SMTP Diagnostic Endpoint ───────────────────────────
    @GetMapping("/api/test-email")
    @ResponseBody
    public String testEmail(@RequestParam(defaultValue = "fantusharyanfkb@gmail.com") String to) {
        if (emailService == null) return "<h3>Error: EmailService is null!</h3>";
        try {
            emailService.testEmailSend(to);
            return "<h3>✅ Success!</h3> Test email sent to " + to + ". Check your inbox/spam.";
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return "<h3>❌ Exception:</h3><p style='color:red'>" + e.getMessage() + "</p><pre>" + sw + "</pre>";
        }
    }
}