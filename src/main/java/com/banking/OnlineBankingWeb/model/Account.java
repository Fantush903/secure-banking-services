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

    @PrePersist
    public void prePersist() {
        if (this.openedDate == null) {
            this.openedDate = LocalDate.now();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
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
}
