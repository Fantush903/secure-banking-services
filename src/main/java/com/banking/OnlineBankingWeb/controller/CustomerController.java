package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.*;
import com.banking.OnlineBankingWeb.repository.*;
import com.banking.OnlineBankingWeb.service.EmailService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private BeneficiaryRepository beneficiaryRepository;
    @Autowired(required = false) private EmailService emailService;
    @Autowired private SecurityLogRepository securityLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // ── Dashboard ────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer user = user(session);

        // Fetch or auto-provision account
        Account account = accountRepository.findByCustomerId(user.getCustomerID());
        if (account == null) {
            account = new Account();
            account.setCustomerId(user.getCustomerID());
            account.setAccountNumber(String.format("1001%012d", user.getCustomerID()));
            account.setAccountType("SAVINGS");
            account.setBalance(50000.00);
            account.setStatus("ACTIVE");
            account.setOpenedDate(LocalDate.now());
            accountRepository.save(account);
        }

        // Fetch top 5 recent transactions
        List<Transaction> transactions = transactionRepository.findTop5ByFromCustomerIdOrderByCreatedAtDesc(user.getCustomerID());

        // Fetch sums
        Double totalDebited = transactionRepository.sumDebitByCustomerId(user.getCustomerID());
        Double totalCredited = transactionRepository.sumCreditByCustomerId(user.getCustomerID());
        List<Transaction> allTx = transactionRepository.findByFromCustomerIdOrderByCreatedAtDesc(user.getCustomerID());

        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalDebited", totalDebited != null ? totalDebited : 0.0);
        model.addAttribute("totalCredited", totalCredited != null ? totalCredited : 0.0);
        model.addAttribute("totalTxCount", allTx.size());

        return "dashboard";
    }

    // ── Fund Transfer ────────────────────────────────────────
    @GetMapping("/transfer")
    public String transferPage(HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        if (account == null) {
            account = new Account();
            account.setCustomerId(u.getCustomerID());
            account.setAccountNumber(String.format("1001%012d", u.getCustomerID()));
            account.setAccountType("SAVINGS");
            account.setBalance(50000.00);
            account.setStatus("ACTIVE");
            account.setOpenedDate(LocalDate.now());
            accountRepository.save(account);
        }
        model.addAttribute("account", account);
        return "transfer";
    }

    @PostMapping("/transfer")
    public String doTransfer(@RequestParam String toAccount, @RequestParam String transferType,
                             @RequestParam double amount, @RequestParam(required=false) String remarks,
                             @RequestParam String tPin, HttpServletRequest request,
                             HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account senderAccount = accountRepository.findByCustomerId(u.getCustomerID());
        
        // Re-add account to model in case of error/success
        model.addAttribute("account", senderAccount);

        if (amount <= 0) { model.addAttribute("error","Amount must be greater than zero."); return "transfer"; }
        if (amount > 100000) { model.addAttribute("error","Daily limit is ₹1,00,000."); return "transfer"; }
        if (toAccount.length() != 16) { model.addAttribute("error","Account number must be 16 digits."); return "transfer"; }
        if (senderAccount == null) { model.addAttribute("error","Sender account not found."); return "transfer"; }
        if (senderAccount.getBalance() < amount) { model.addAttribute("error","Insufficient balance."); return "transfer"; }

        // T-PIN Validation
        if (tPin == null || tPin.isEmpty()) {
            model.addAttribute("error", "Transaction PIN (T-PIN) is required.");
            return "transfer";
        }
        if (senderAccount.gettPin() == null || !passwordEncoder.matches(tPin, senderAccount.gettPin())) {
            model.addAttribute("error", "Invalid Transaction PIN (T-PIN).");
            return "transfer";
        }

        // Card Control Checks
        if ("BLOCKED".equalsIgnoreCase(senderAccount.getCardStatus())) {
            model.addAttribute("error", "Transaction declined: Debit card is blocked.");
            return "transfer";
        }
        if (!senderAccount.getOnlineEnabled()) {
            model.addAttribute("error", "Transaction declined: Online transactions are disabled on this card.");
            return "transfer";
        }
        if (amount > senderAccount.getPosLimit()) {
            model.addAttribute("error", "Transaction declined: Amount exceeds card's daily POS limit (₹" + fmt(senderAccount.getPosLimit()) + ").");
            return "transfer";
        }
        
        // Check if destination account is internal
        Account receiverAccount = accountRepository.findByAccountNumber(toAccount);
        
        // Deduct from sender
        senderAccount.setBalance(senderAccount.getBalance() - amount);
        accountRepository.save(senderAccount);
        
        // Record debit transaction for sender
        Transaction debitTx = new Transaction();
        debitTx.setFromCustomerId(u.getCustomerID());
        debitTx.setToAccount(toAccount);
        debitTx.setAmount(amount);
        debitTx.setType("DEBIT");
        debitTx.setTransferType(transferType);
        debitTx.setDescription("Fund Transfer to " + toAccount);
        debitTx.setBalanceAfter(senderAccount.getBalance());
        debitTx.setRemarks(remarks);
        transactionRepository.save(debitTx);
        
        if (receiverAccount != null) {
            // Credit receiver
            receiverAccount.setBalance(receiverAccount.getBalance() + amount);
            accountRepository.save(receiverAccount);
            
            // Record credit transaction for receiver
            Transaction creditTx = new Transaction();
            creditTx.setFromCustomerId(receiverAccount.getCustomerId());
            creditTx.setToAccount(toAccount);
            creditTx.setAmount(amount);
            creditTx.setType("CREDIT");
            creditTx.setTransferType(transferType);
            creditTx.setDescription("Fund Transfer from " + senderAccount.getAccountNumber());
            creditTx.setBalanceAfter(receiverAccount.getBalance());
            creditTx.setRemarks(remarks);
            transactionRepository.save(creditTx);
        }
        
        if (emailService != null) {
            emailService.sendTransferConfirmation(u.getEmail(), u.getName(), toAccount, amount, transferType);
        }
        
        // Log successful transfer
        SecurityLog log = new SecurityLog();
        log.setCustomerId(u.getCustomerID());
        log.setAction("FUND_TRANSFER");
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        securityLogRepository.save(log);

        model.addAttribute("success","✅ ₹"+fmt(amount)+" transferred to "+toAccount+" via "+transferType+"!");
        return "transfer";
    }


    // ── UPI ──────────────────────────────────────────────────
    @GetMapping("/upi")
    public String upiPage(HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        if (account == null) {
            account = new Account();
            account.setCustomerId(u.getCustomerID());
            account.setAccountNumber(String.format("1001%012d", u.getCustomerID()));
            account.setAccountType("SAVINGS");
            account.setBalance(50000.00);
            account.setStatus("ACTIVE");
            account.setOpenedDate(LocalDate.now());
            accountRepository.save(account);
        }
        model.addAttribute("account", account);
        return "upi";
    }

    @PostMapping("/upi-send")
    public String upiSend(@RequestParam String toUpiId, @RequestParam double amount,
                          @RequestParam(required=false) String note, @RequestParam String tPin,
                          HttpServletRequest request, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account senderAccount = accountRepository.findByCustomerId(u.getCustomerID());
        model.addAttribute("account", senderAccount);

        if (!toUpiId.contains("@")) { model.addAttribute("error","Invalid UPI ID."); return "upi"; }
        if (amount <= 0) { model.addAttribute("error","Amount must be > 0."); return "upi"; }
        if (senderAccount == null) { model.addAttribute("error","Account not found."); return "upi"; }
        if (senderAccount.getBalance() < amount) { model.addAttribute("error","Insufficient balance."); return "upi"; }

        // T-PIN Validation
        if (tPin == null || tPin.isEmpty()) {
            model.addAttribute("error", "Transaction PIN (T-PIN) is required.");
            return "upi";
        }
        if (senderAccount.gettPin() == null || !passwordEncoder.matches(tPin, senderAccount.gettPin())) {
            model.addAttribute("error", "Invalid Transaction PIN (T-PIN).");
            return "upi";
        }

        // Card Control Checks
        if ("BLOCKED".equalsIgnoreCase(senderAccount.getCardStatus())) {
            model.addAttribute("error", "Transaction declined: Debit card is blocked.");
            return "upi";
        }
        if (!senderAccount.getOnlineEnabled()) {
            model.addAttribute("error", "Transaction declined: Online transactions are disabled on this card.");
            return "upi";
        }
        if (amount > senderAccount.getPosLimit()) {
            model.addAttribute("error", "Transaction declined: Amount exceeds card's daily POS/UPI limit (₹" + fmt(senderAccount.getPosLimit()) + ").");
            return "upi";
        }

        // Deduct balance
        senderAccount.setBalance(senderAccount.getBalance() - amount);
        accountRepository.save(senderAccount);

        // Record debit transaction
        Transaction debitTx = new Transaction();
        debitTx.setFromCustomerId(u.getCustomerID());
        debitTx.setToAccount(toUpiId);
        debitTx.setAmount(amount);
        debitTx.setType("DEBIT");
        debitTx.setTransferType("UPI");
        debitTx.setDescription("UPI Send to " + toUpiId);
        debitTx.setBalanceAfter(senderAccount.getBalance());
        debitTx.setRemarks(note);
        transactionRepository.save(debitTx);

        if (emailService != null) {
            emailService.sendTransferConfirmation(u.getEmail(), u.getName(), toUpiId, amount, "UPI");
        }

        // Log successful transfer
        SecurityLog log = new SecurityLog();
        log.setCustomerId(u.getCustomerID());
        log.setAction("UPI_TRANSFER");
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        securityLogRepository.save(log);

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
    public String transactions(HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        List<Transaction> transactions = transactionRepository.findByFromCustomerIdOrderByCreatedAtDesc(u.getCustomerID());
        
        Double totalDebited = transactionRepository.sumDebitByCustomerId(u.getCustomerID());
        Double totalCredited = transactionRepository.sumCreditByCustomerId(u.getCustomerID());
        
        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalDebited", totalDebited != null ? totalDebited : 0.0);
        model.addAttribute("totalCredited", totalCredited != null ? totalCredited : 0.0);
        model.addAttribute("totalTxCount", transactions.size());
        return "transactions";
    }

    // ── Loans ────────────────────────────────────────────────
    @GetMapping("/loans")
    public String loansPage(HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        List<Loan> loans = loanRepository.findByCustomerIdOrderByAppliedAtDesc(u.getCustomerID());
        model.addAttribute("loans", loans);
        return "loans";
    }

    @PostMapping("/apply-loan")
    public String applyLoan(@RequestParam String loanType, @RequestParam double amount,
                            @RequestParam int tenure, @RequestParam(required=false) String purpose,
                            @RequestParam(required=false) String income, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        
        if (amount < 10000) {
            model.addAttribute("error","Minimum loan ₹10,000.");
            model.addAttribute("loans", loanRepository.findByCustomerIdOrderByAppliedAtDesc(u.getCustomerID()));
            return "loans";
        }

        double rate = 8.5;
        if ("PERSONAL".equalsIgnoreCase(loanType)) rate = 12.0;
        else if ("EDUCATION".equalsIgnoreCase(loanType)) rate = 9.0;

        // Calculate EMI
        double monthlyRate = rate / 100.0 / 12.0;
        double emi = amount * monthlyRate * Math.pow(1 + monthlyRate, tenure) / (Math.pow(1 + monthlyRate, tenure) - 1);
        if (Double.isNaN(emi) || Double.isInfinite(emi)) {
            emi = amount / tenure;
        }

        double incomeVal = 0.0;
        try {
            if (income != null && !income.trim().isEmpty()) {
                incomeVal = Double.parseDouble(income);
            }
        } catch (NumberFormatException e) {
            // ignore
        }

        Loan loan = new Loan();
        loan.setCustomerId(u.getCustomerID());
        loan.setLoanType(loanType);
        loan.setAmount(amount);
        loan.setTenureMonths(tenure);
        loan.setInterestRate(rate);
        loan.setEmiAmount(emi);
        loan.setPurpose(purpose);
        loan.setMonthlyIncome(incomeVal);
        loan.setStatus("PENDING");
        loanRepository.save(loan);

        model.addAttribute("success","✅ "+loanType+" loan for ₹"+fmt(amount)+" submitted! Response in 2-3 days.");
        model.addAttribute("loans", loanRepository.findByCustomerIdOrderByAppliedAtDesc(u.getCustomerID()));
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
    public String beneficiariesPage(HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        List<Beneficiary> beneficiaries = beneficiaryRepository.findByCustomerId(u.getCustomerID());
        model.addAttribute("beneficiaries", beneficiaries);
        return "beneficiaries";
    }

    @PostMapping("/add-beneficiary")
    public String addBeneficiary(@RequestParam String name, @RequestParam String accountNo,
                                 @RequestParam String bankName, @RequestParam(required=false) String ifsc,
                                 @RequestParam(required=false) String nickname, @RequestParam(required=false) String mobile,
                                 HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        if (accountNo.length() != 16) {
            model.addAttribute("error","Account must be 16 digits.");
            model.addAttribute("beneficiaries", beneficiaryRepository.findByCustomerId(u.getCustomerID()));
            return "beneficiaries";
        }

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setCustomerId(u.getCustomerID());
        beneficiary.setName(name);
        beneficiary.setAccountNo(accountNo);
        beneficiary.setBankName(bankName);
        beneficiary.setIfsc(ifsc);
        beneficiary.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname : null);
        beneficiary.setMobile(mobile);
        beneficiaryRepository.save(beneficiary);

        model.addAttribute("success","✅ Beneficiary "+name+" added!");
        model.addAttribute("beneficiaries", beneficiaryRepository.findByCustomerId(u.getCustomerID()));
        return "beneficiaries";
    }

    @GetMapping("/delete-beneficiary/{id}")
    public String deleteBeneficiary(@PathVariable int id, HttpSession session) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        beneficiaryRepository.findById(id).ifPresent(b -> {
            if (b.getCustomerId() == u.getCustomerID()) {
                beneficiaryRepository.delete(b);
            }
        });
        return "redirect:/customer/beneficiaries";
    }

    // ── Currency ─────────────────────────────────────────────
    @GetMapping("/currency")
    public String currencyPage(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "currency"; }

    @GetMapping("/card")
    public String cardPage(HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        if (account == null) {
            account = new Account();
            account.setCustomerId(u.getCustomerID());
            account.setAccountNumber(String.format("1001%012d", u.getCustomerID()));
            account.setAccountType("SAVINGS");
            account.setBalance(50000.00);
            account.setStatus("ACTIVE");
            account.setOpenedDate(LocalDate.now());
            accountRepository.save(account);
        }
        model.addAttribute("account", account);
        return "card";
    }

    @PostMapping("/update-card-limits")
    public String updateCardLimits(@RequestParam double atmLimit, @RequestParam double posLimit,
                                   HttpServletRequest request, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        if (account != null) {
            account.setAtmLimit(atmLimit);
            account.setPosLimit(posLimit);
            accountRepository.save(account);

            // Log limit update
            SecurityLog log = new SecurityLog();
            log.setCustomerId(u.getCustomerID());
            log.setAction("CARD_LIMIT_UPDATE");
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            securityLogRepository.save(log);

            model.addAttribute("success", "✅ ATM limit: ₹" + fmt(atmLimit) + " | POS limit: ₹" + fmt(posLimit) + " updated!");
        }
        model.addAttribute("account", account);
        return "card";
    }

    @PostMapping("/change-pin")
    public String changePin(@RequestParam String currentPin, @RequestParam String newPin,
                            @RequestParam String confirmPin, HttpServletRequest request,
                            HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        model.addAttribute("account", account);

        if (!newPin.equals(confirmPin)) { model.addAttribute("error", "PINs do not match."); return "card"; }
        if (newPin.length() != 4) { model.addAttribute("error", "PIN must be 4 digits."); return "card"; }
        if (account == null) { model.addAttribute("error", "Account not found."); return "card"; }

        if (account.getCardPin() == null || !passwordEncoder.matches(currentPin, account.getCardPin())) {
            model.addAttribute("error", "Current PIN is incorrect.");
            return "card";
        }

        account.setCardPin(passwordEncoder.encode(newPin));
        accountRepository.save(account);

        // Log PIN change
        SecurityLog log = new SecurityLog();
        log.setCustomerId(u.getCustomerID());
        log.setAction("CARD_PIN_CHANGE");
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        securityLogRepository.save(log);

        model.addAttribute("success", "✅ Card PIN changed successfully!");
        return "card";
    }

    @PostMapping("/block-card")
    public String blockCard(HttpServletRequest request, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        if (account != null) {
            String newStatus = "ACTIVE".equalsIgnoreCase(account.getCardStatus()) ? "BLOCKED" : "ACTIVE";
            account.setCardStatus(newStatus);
            accountRepository.save(account);

            // Log card block state change
            SecurityLog log = new SecurityLog();
            log.setCustomerId(u.getCustomerID());
            log.setAction("CARD_STATUS_CHANGE");
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            securityLogRepository.save(log);

            model.addAttribute("success", "✅ Card has been " + ("BLOCKED".equalsIgnoreCase(newStatus) ? "temporarily blocked" : "unblocked") + "!");
        }
        model.addAttribute("account", account);
        return "card";
    }

    @PostMapping("/toggle-card-feature")
    public String toggleCardFeature(@RequestParam String feature, HttpServletRequest request,
                                    HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        if (account != null) {
            if ("online".equalsIgnoreCase(feature)) {
                account.setOnlineEnabled(!account.getOnlineEnabled());
            } else if ("intl".equalsIgnoreCase(feature)) {
                account.setIntlEnabled(!account.getIntlEnabled());
            } else if ("contactless".equalsIgnoreCase(feature)) {
                account.setContactlessEnabled(!account.getContactlessEnabled());
            } else if ("atm".equalsIgnoreCase(feature)) {
                account.setAtmEnabled(!account.getAtmEnabled());
            }
            accountRepository.save(account);

            // Log log feature toggle
            SecurityLog log = new SecurityLog();
            log.setCustomerId(u.getCustomerID());
            log.setAction("CARD_FEATURE_TOGGLE");
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            securityLogRepository.save(log);
        }
        model.addAttribute("account", account);
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
        Customer u = user(session);
        List<SecurityLog> logs = securityLogRepository.findByCustomerIdOrderByCreatedAtDesc(u.getCustomerID());
        if (logs.size() > 10) {
            logs = logs.subList(0, 10);
        }
        model.addAttribute("user", u);
        model.addAttribute("securityLogs", logs);
        return "profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, HttpServletRequest request,
                                 HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        
        List<SecurityLog> logs = securityLogRepository.findByCustomerIdOrderByCreatedAtDesc(u.getCustomerID());
        if (logs.size() > 10) logs = logs.subList(0, 10);
        model.addAttribute("securityLogs", logs);
        model.addAttribute("user", u);

        if (u.getPassword() == null || !passwordEncoder.matches(currentPassword, u.getPassword())) {
            model.addAttribute("error", "Current password incorrect.");
            return "profile";
        }
        if (!newPassword.equals(confirmPassword)) { model.addAttribute("error", "Passwords do not match."); return "profile"; }
        if (newPassword.length() < 8) { model.addAttribute("error", "Min 8 characters."); return "profile"; }
        
        u.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(u);
        session.setAttribute("user", u);

        // Log password change
        SecurityLog log = new SecurityLog();
        log.setCustomerId(u.getCustomerID());
        log.setAction("PASSWORD_CHANGE");
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        securityLogRepository.save(log);

        // Reload logs
        logs = securityLogRepository.findByCustomerIdOrderByCreatedAtDesc(u.getCustomerID());
        if (logs.size() > 10) logs = logs.subList(0, 10);
        model.addAttribute("securityLogs", logs);

        model.addAttribute("success", "✅ Password changed!");
        return "profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@RequestParam String name, @RequestParam String phone,
                                @RequestParam(required=false) String address, HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        
        List<SecurityLog> logs = securityLogRepository.findByCustomerIdOrderByCreatedAtDesc(u.getCustomerID());
        if (logs.size() > 10) logs = logs.subList(0, 10);
        model.addAttribute("securityLogs", logs);
        model.addAttribute("user", u);

        if (phone.length() != 10) { model.addAttribute("error", "Phone must be 10 digits."); return "profile"; }
        u.setName(name); u.setPhone(phone); u.setAddress(address);
        customerRepository.save(u);
        session.setAttribute("user", u);
        
        model.addAttribute("success", "✅ Profile updated!");
        return "profile";
    }

    @PostMapping("/update-tpin")
    public String updateTPin(@RequestParam String currentPin, @RequestParam String newPin,
                             @RequestParam String confirmPin, HttpServletRequest request,
                             HttpSession session, Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        
        List<SecurityLog> logs = securityLogRepository.findByCustomerIdOrderByCreatedAtDesc(u.getCustomerID());
        if (logs.size() > 10) logs = logs.subList(0, 10);
        model.addAttribute("securityLogs", logs);
        model.addAttribute("user", u);

        if (!newPin.equals(confirmPin)) { model.addAttribute("error", "T-PINs do not match."); return "profile"; }
        if (newPin.length() != 4) { model.addAttribute("error", "T-PIN must be 4 digits."); return "profile"; }
        if (account == null) { model.addAttribute("error", "Account not found."); return "profile"; }

        if (account.gettPin() != null && !passwordEncoder.matches(currentPin, account.gettPin())) {
            model.addAttribute("error", "Current Transaction PIN is incorrect.");
            return "profile";
        }

        account.settPin(passwordEncoder.encode(newPin));
        accountRepository.save(account);

        // Log TPIN update
        SecurityLog log = new SecurityLog();
        log.setCustomerId(u.getCustomerID());
        log.setAction("TPIN_CHANGE");
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        securityLogRepository.save(log);

        // Reload logs
        logs = securityLogRepository.findByCustomerIdOrderByCreatedAtDesc(u.getCustomerID());
        if (logs.size() > 10) logs = logs.subList(0, 10);
        model.addAttribute("securityLogs", logs);

        model.addAttribute("success", "✅ Transaction PIN (T-PIN) updated successfully!");
        return "profile";
    }

    // ── Statement & Export ───────────────────────────────────
    @GetMapping("/statement")
    public String statement(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpSession session,
            Model model) {
        if (!ok(session)) return "redirect:/login";
        Customer u = user(session);
        Account account = accountRepository.findByCustomerId(u.getCustomerID());
        if (account == null) {
            account = new Account();
            account.setCustomerId(u.getCustomerID());
            account.setAccountNumber(String.format("1001%012d", u.getCustomerID()));
            account.setAccountType("SAVINGS");
            account.setBalance(50000.00);
            account.setStatus("ACTIVE");
            account.setOpenedDate(LocalDate.now());
            accountRepository.save(account);
        }

        List<Transaction> transactions = transactionRepository.findByFromCustomerIdOrderByCreatedAtDesc(u.getCustomerID());
        
        LocalDate fromDate = null;
        LocalDate toDate = null;
        if (from != null && !from.trim().isEmpty()) {
            try {
                fromDate = LocalDate.parse(from);
            } catch (Exception e) {}
        }
        if (to != null && !to.trim().isEmpty()) {
            try {
                toDate = LocalDate.parse(to);
            } catch (Exception e) {}
        }

        java.util.ArrayList<Transaction> filteredTransactions = new java.util.ArrayList<>();
        double totalDebited = 0.0;
        double totalCredited = 0.0;

        for (Transaction tx : transactions) {
            LocalDate txDate = tx.getCreatedAt().toLocalDate();
            boolean keep = true;
            if (fromDate != null && txDate.isBefore(fromDate)) {
                keep = false;
            }
            if (toDate != null && txDate.isAfter(toDate)) {
                keep = false;
            }
            if (keep) {
                filteredTransactions.add(tx);
                if ("DEBIT".equalsIgnoreCase(tx.getType())) {
                    totalDebited += tx.getAmount();
                } else if ("CREDIT".equalsIgnoreCase(tx.getType())) {
                    totalCredited += tx.getAmount();
                }
            }
        }

        double closingBalance = account.getBalance();
        double openingBalance = closingBalance - totalCredited + totalDebited;

        model.addAttribute("account", account);
        model.addAttribute("transactions", filteredTransactions);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("totalDebited", totalDebited);
        model.addAttribute("totalCredited", totalCredited);
        model.addAttribute("openingBalance", openingBalance);
        model.addAttribute("closingBalance", closingBalance);

        return "statement";
    }

    // ── Help ─────────────────────────────────────────────────
    @GetMapping("/help")
    public String help(HttpSession session) { if (!ok(session)) return "redirect:/login"; return "help"; }

    // ── Helpers ──────────────────────────────────────────────
    private boolean ok(HttpSession s) { return s.getAttribute("user") != null; }
    private Customer user(HttpSession s) { return (Customer) s.getAttribute("user"); }
    private String fmt(double amount) { return String.format("%.2f", amount); }
}