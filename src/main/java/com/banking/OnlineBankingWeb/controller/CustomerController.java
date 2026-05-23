package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.Customer;
import com.banking.OnlineBankingWeb.repository.CustomerRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired private CustomerRepository customerRepository;

    // ── Dashboard ────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        if (!ok(session)) return "redirect:/login";
        return "dashboard";
    }

    // ── Fund Transfer ────────────────────────────────────────
    @GetMapping("/transfer")
    public String transferPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "transfer"; }

    @PostMapping("/transfer")
    public String doTransfer(@RequestParam String toAccount, @RequestParam String transferType,
                             @RequestParam double amount, @RequestParam(required=false) String remarks,
                             HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        if (amount <= 0) { model.addAttribute("error","Amount must be greater than zero."); return "transfer"; }
        if (amount > 100000) { model.addAttribute("error","Daily limit is ₹1,00,000."); return "transfer"; }
        if (toAccount.length() != 16) { model.addAttribute("error","Account number must be 16 digits."); return "transfer"; }
        System.out.println("TRANSFER: "+u.getEmail()+" → "+toAccount+" ₹"+amount+" ["+transferType+"]");
        model.addAttribute("success","✅ ₹"+fmt(amount)+" transferred to "+toAccount+" via "+transferType+"!");
        return "transfer";
    }

    // ── UPI ──────────────────────────────────────────────────
    @GetMapping("/upi")
    public String upiPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "upi"; }

    @PostMapping("/upi-send")
    public String upiSend(@RequestParam String toUpiId, @RequestParam double amount,
                          @RequestParam(required=false) String note, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        if (!toUpiId.contains("@")) { model.addAttribute("error","Invalid UPI ID."); return "upi"; }
        if (amount <= 0) { model.addAttribute("error","Amount must be > 0."); return "upi"; }
        model.addAttribute("success","✅ ₹"+fmt(amount)+" sent to "+toUpiId+"!");
        return "upi";
    }

    @PostMapping("/upi-request")
    public String upiRequest(@RequestParam String fromUpiId, @RequestParam double amount,
                             @RequestParam String reason, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        model.addAttribute("success","✅ Request of ₹"+fmt(amount)+" sent to "+fromUpiId+"!");
        return "upi";
    }

    // ── Transactions ─────────────────────────────────────────
    @GetMapping("/transactions")
    public String transactions(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "transactions"; }

    // ── Loans ────────────────────────────────────────────────
    @GetMapping("/loans")
    public String loansPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "loans"; }

    @PostMapping("/apply-loan")
    public String applyLoan(@RequestParam String loanType, @RequestParam double amount,
                            @RequestParam int tenure, @RequestParam(required=false) String purpose,
                            @RequestParam(required=false) String income, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        if (amount < 10000) { model.addAttribute("error","Minimum loan ₹10,000."); return "loans"; }
        model.addAttribute("success","✅ "+loanType+" loan for ₹"+fmt(amount)+" submitted! Response in 2-3 days.");
        return "loans";
    }

    // ── Bills ────────────────────────────────────────────────
    @GetMapping("/bills")
    public String billsPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "bills"; }

    @PostMapping("/pay-bill")
    public String payBill(@RequestParam String billType, @RequestParam String consumerNo,
                          @RequestParam String provider, @RequestParam double amount,
                          @RequestParam(required=false) String billMonth, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        model.addAttribute("success","✅ "+billType+" bill ₹"+fmt(amount)+" paid to "+provider+"!");
        return "bills";
    }

    // ── Fixed Deposit ────────────────────────────────────────
    @GetMapping("/fd")
    public String fdPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "fd"; }

    @PostMapping("/open-fd")
    public String openFD(@RequestParam double amount, @RequestParam int tenure,
                         @RequestParam String payout, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        if (amount < 1000) { model.addAttribute("error","Min FD ₹1,000."); return "fd"; }
        model.addAttribute("success","✅ FD of ₹"+fmt(amount)+" opened for "+tenure+" days!");
        return "fd";
    }

    // ── Recurring Deposit ────────────────────────────────────
    @GetMapping("/rd")
    public String rdPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "rd"; }

    @PostMapping("/open-rd")
    public String openRD(@RequestParam double monthlyAmount, @RequestParam int tenure,
                         @RequestParam String debitDate, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        if (monthlyAmount < 500) { model.addAttribute("error","Min RD ₹500/month."); return "rd"; }
        model.addAttribute("success","✅ RD of ₹"+fmt(monthlyAmount)+"/month opened for "+tenure+" months!");
        return "rd";
    }

    // ── Beneficiaries ────────────────────────────────────────
    @GetMapping("/beneficiaries")
    public String beneficiariesPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "beneficiaries"; }

    @PostMapping("/add-beneficiary")
    public String addBeneficiary(@RequestParam String name, @RequestParam String accountNo,
                                 @RequestParam String bankName, @RequestParam(required=false) String ifsc,
                                 @RequestParam(required=false) String nickname, @RequestParam(required=false) String mobile,
                                 HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        if (accountNo.length() != 16) { model.addAttribute("error","Account must be 16 digits."); return "beneficiaries"; }
        model.addAttribute("success","✅ Beneficiary "+name+" added!");
        return "beneficiaries";
    }

    @GetMapping("/delete-beneficiary/{id}")
    public String deleteBeneficiary(@PathVariable int id, HttpSession session) {
        if (!ok(session)) return "redirect:/login";
        return "redirect:/customer/beneficiaries";
    }

    // ── Currency ─────────────────────────────────────────────
    @GetMapping("/currency")
    public String currencyPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "currency"; }

    // ── Debit Card ───────────────────────────────────────────
    @GetMapping("/card")
    public String cardPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "card"; }

    @PostMapping("/update-card-limits")
    public String updateCardLimits(@RequestParam int atmLimit, @RequestParam int posLimit,
                                   HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        model.addAttribute("success","✅ ATM limit: ₹"+atmLimit+" | POS limit: ₹"+posLimit+" updated!");
        return "card";
    }

    @PostMapping("/change-pin")
    public String changePin(@RequestParam String currentPin, @RequestParam String newPin,
                            @RequestParam String confirmPin, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        if (!newPin.equals(confirmPin)) { model.addAttribute("error","PINs do not match."); return "card"; }
        if (newPin.length() != 4) { model.addAttribute("error","PIN must be 4 digits."); return "card"; }
        model.addAttribute("success","✅ Card PIN changed successfully!");
        return "card";
    }

    // ── Standing Instructions ────────────────────────────────
    @GetMapping("/standing-instructions")
    public String standingInstructionsPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "standing-instructions"; }

    @PostMapping("/add-standing-instruction")
    public String addStandingInstruction(@RequestParam String name, @RequestParam String toAccount,
                                         @RequestParam double amount, @RequestParam String frequency,
                                         @RequestParam String startDate, @RequestParam(required=false) String endDate,
                                         @RequestParam(required=false) String category, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        model.addAttribute("success","✅ Standing instruction '"+name+"' for ₹"+fmt(amount)+" "+frequency.toLowerCase()+" created!");
        return "standing-instructions";
    }

    // ── Nominee ──────────────────────────────────────────────
    @GetMapping("/nominee")
    public String nomineePage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "nominee"; }

    @PostMapping("/add-nominee")
    public String addNominee(@RequestParam String nomineeName, @RequestParam String relationship,
                             @RequestParam String dob, @RequestParam(required=false) String phone,
                             @RequestParam String aadhaar, @RequestParam int sharePercent,
                             @RequestParam(required=false) String address, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        if (aadhaar.length() != 12) { model.addAttribute("error","Aadhaar must be 12 digits."); return "nominee"; }
        model.addAttribute("success","✅ Nominee "+nomineeName+" ("+relationship+") with "+sharePercent+"% share added!");
        return "nominee";
    }

    // ── Cheque Book ──────────────────────────────────────────
    @GetMapping("/cheque")
    public String chequePage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "cheque"; }

    @PostMapping("/request-cheque")
    public String requestCheque(@RequestParam String chequeType, @RequestParam String addressType,
                                @RequestParam(required=false) String customAddress, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        model.addAttribute("success","✅ "+chequeType.replace("_"," ")+" cheque book requested! Delivery in 5-7 working days.");
        return "cheque";
    }

    // ── Service Request ──────────────────────────────────────
    @GetMapping("/service-request")
    public String serviceRequestPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "service-request"; }

    @PostMapping("/submit-service-request")
    public String submitServiceRequest(@RequestParam String serviceType,
                                       @RequestParam(required=false) String description, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        model.addAttribute("success","✅ Service request for '"+serviceType+"' submitted! Ref: SR"+System.currentTimeMillis());
        return "service-request";
    }

    // ── Complaints ───────────────────────────────────────────
    @GetMapping("/complaints")
    public String complaintsPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "complaints"; }

    @PostMapping("/submit-complaint")
    public String submitComplaint(@RequestParam String category, @RequestParam String subject,
                                  @RequestParam String description, @RequestParam String priority,
                                  @RequestParam(required=false) String transactionId, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        String ref = "CMP"+System.currentTimeMillis();
        model.addAttribute("success","✅ Complaint submitted! Reference: "+ref+". We'll respond in 48 hours.");
        return "complaints";
    }

    // ── Secure Inbox ─────────────────────────────────────────
    @GetMapping("/inbox")
    public String inboxPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "inbox"; }

    // ── KYC ──────────────────────────────────────────────────
    @GetMapping("/kyc")
    public String kycPage(HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        model.addAttribute("user", session.getAttribute("user"));
        return "kyc";
    }

    @PostMapping("/update-kyc")
    public String updateKyc(@RequestParam(required=false) String aadhaar, @RequestParam(required=false) String pan,
                            @RequestParam(required=false) String dob, @RequestParam(required=false) String occupation,
                            @RequestParam(required=false) String address, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        if (address != null && !address.isEmpty()) {
            u.setAddress(address);
            customerRepository.save(u);
            session.setAttribute("user", u);
        }
        model.addAttribute("success","✅ KYC update request submitted! Documents under review.");
        model.addAttribute("user", u);
        return "kyc";
    }

    // ── Profile ──────────────────────────────────────────────
    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        model.addAttribute("user", session.getAttribute("user"));
        return "profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        if (!u.getPassword().equals(currentPassword)) { model.addAttribute("error","Current password incorrect."); model.addAttribute("user",u); return "profile"; }
        if (!newPassword.equals(confirmPassword)) { model.addAttribute("error","Passwords do not match."); model.addAttribute("user",u); return "profile"; }
        if (newPassword.length() < 8) { model.addAttribute("error","Min 8 characters."); model.addAttribute("user",u); return "profile"; }
        u.setPassword(newPassword); customerRepository.save(u); session.setAttribute("user",u);
        model.addAttribute("success","✅ Password changed!"); model.addAttribute("user",u);
        return "profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@RequestParam String name, @RequestParam String phone,
                                @RequestParam(required=false) String address, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        if (phone.length() != 10) { model.addAttribute("error","Phone must be 10 digits."); model.addAttribute("user",u); return "profile"; }
        u.setName(name); u.setPhone(phone); u.setAddress(address);
        customerRepository.save(u); session.setAttribute("user",u);
        model.addAttribute("success","✅ Profile updated!"); model.addAttribute("user",u);
        return "profile";
    }

    // ── Statement & Export ───────────────────────────────────
    @GetMapping("/statement")
    public String statement(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "statement"; }

    // ── Notifications ────────────────────────────────────────
    @GetMapping("/notifications")
    public String notifications(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "notifications"; }

    // ── Help ─────────────────────────────────────────────────
    @GetMapping("/help")
    public String help(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "help"; }

    // ── Helpers ──────────────────────────────────────────────
    private boolean ok(HttpSession s) { return s.getAttribute("user") != null; }
    private Customer user(HttpSession s) { return (Customer) s.getAttribute("user"); }
    private String fmt(double amount) { return String.format("%.2f", amount); }
}