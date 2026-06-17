package com.banking.OnlineBankingWeb.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID")
    private int accountId;

    @Column(name = "CustomerID")
    private int customerId;

    @Column(name = "BranchID")
    private int branchId = 1;

    @Column(name = "AccountNumber", unique = true)
    private String accountNumber;

    @Column(name = "AccountType")
    private String accountType; // SAVINGS, CURRENT

    @Column(name = "Balance")
    private double balance;

    @Column(name = "Status")
    private String status; // PENDING, ACTIVE, INACTIVE, CLOSED

    @Column(name = "OpenedDate")
    private LocalDate openedDate;

    @Column(name = "t_pin")
    private String tPin;

    @Column(name = "card_pin")
    private String cardPin;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "card_cvv")
    private String cardCvv;

    @Column(name = "card_expiry")
    private String cardExpiry;

    @Column(name = "card_status")
    private String cardStatus; // ACTIVE, BLOCKED

    @Column(name = "atm_limit")
    private double atmLimit = 10000.00;

    @Column(name = "pos_limit")
    private double posLimit = 50000.00;

    @Column(name = "online_enabled")
    private Boolean onlineEnabled = true;

    @Column(name = "intl_enabled")
    private Boolean intlEnabled = false;

    @Column(name = "contactless_enabled")
    private Boolean contactlessEnabled = true;

    @Column(name = "atm_enabled")
    private Boolean atmEnabled = true;

    @PrePersist
    public void prePersist() {
        if (this.openedDate == null) {
            this.openedDate = LocalDate.now();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        if (this.cardNumber == null) {
            int random1 = (int)(Math.random() * 9000) + 1000;
            int random2 = (int)(Math.random() * 9000) + 1000;
            this.cardNumber = String.format("4312-8877-%04d-%04d", random1, random2);
        }
        if (this.cardCvv == null) {
            this.cardCvv = String.format("%03d", (int)(Math.random() * 900) + 100);
        }
        if (this.cardExpiry == null) {
            int month = LocalDate.now().getMonthValue();
            int year = (LocalDate.now().getYear() + 5) % 100;
            this.cardExpiry = String.format("%02d/%02d", month, year);
        }
        if (this.cardStatus == null) {
            this.cardStatus = "ACTIVE";
        }
        if (this.cardPin == null) {
            // default '1234'
            this.cardPin = "$2a$10$vD2pG40.Dbe.3Qp/y7iTaei02b2/JjX/Wl87s73dJm9CplH.N7tJ2";
        }
    }

    // ── Getters & Setters ────────────────────────────────────
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getOpenedDate() { return openedDate; }
    public void setOpenedDate(LocalDate openedDate) { this.openedDate = openedDate; }

    public String gettPin() { return tPin; }
    public void settPin(String tPin) { this.tPin = tPin; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardCvv() { return cardCvv; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }

    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }

    public String getCardStatus() { return cardStatus; }
    public void setCardStatus(String cardStatus) { this.cardStatus = cardStatus; }

    public double getAtmLimit() { return atmLimit; }
    public void setAtmLimit(double atmLimit) { this.atmLimit = atmLimit; }

    public double getPosLimit() { return posLimit; }
    public void setPosLimit(double posLimit) { this.posLimit = posLimit; }

    public Boolean getOnlineEnabled() { return onlineEnabled; }
    public void setOnlineEnabled(Boolean onlineEnabled) { this.onlineEnabled = onlineEnabled; }

    public Boolean getIntlEnabled() { return intlEnabled; }
    public void setIntlEnabled(Boolean intlEnabled) { this.intlEnabled = intlEnabled; }

    public Boolean getContactlessEnabled() { return contactlessEnabled; }
    public void setContactlessEnabled(Boolean contactlessEnabled) { this.contactlessEnabled = contactlessEnabled; }

    public Boolean getAtmEnabled() { return atmEnabled; }
    public void setAtmEnabled(Boolean atmEnabled) { this.atmEnabled = atmEnabled; }

    public String getCardPin() { return cardPin; }
    public void setCardPin(String cardPin) { this.cardPin = cardPin; }
}
