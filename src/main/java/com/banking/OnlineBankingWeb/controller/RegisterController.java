package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.Customer;
import com.banking.OnlineBankingWeb.repository.CustomerRepository;
import com.banking.OnlineBankingWeb.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegisterController {

    @Autowired private CustomerRepository customerRepository;
    @Autowired(required = false) private EmailService emailService;

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String registerSubmit(
            @RequestParam String name, @RequestParam String email,
            @RequestParam String phone, @RequestParam(required = false) String address,
            @RequestParam(required = false) String dob, @RequestParam String aadhaar,
            @RequestParam String pan, @RequestParam String password,
            @RequestParam String confirmPassword, Model model) {

        if (!password.equals(confirmPassword)) { model.addAttribute("error", "Passwords do not match."); return "register"; }
        if (password.length() < 8) { model.addAttribute("error", "Password must be at least 8 characters."); return "register"; }
        if (customerRepository.findByEmail(email) != null) { model.addAttribute("error", "Account already exists. Please login."); return "register"; }
        if (aadhaar.length() != 12) { model.addAttribute("error", "Aadhaar must be 12 digits."); return "register"; }
        if (pan.length() != 10) { model.addAttribute("error", "PAN must be 10 characters."); return "register"; }
        if (phone.length() != 10) { model.addAttribute("error", "Phone must be 10 digits."); return "register"; }

        Customer customer = new Customer();
        customer.setName(name); customer.setEmail(email); customer.setPhone(phone);
        customer.setAddress(address); customer.setPassword(password);
        customer.setRole("CUSTOMER"); customer.setStatus("ACTIVE"); customer.setFailedLogins(0);
        customerRepository.save(customer);

        if (emailService != null) emailService.sendWelcome(email, name);
        System.out.println("=== NEW REGISTRATION: " + email + " ===");

        return "redirect:/login?success=Account+created+successfully!+Please+login.";
    }
}