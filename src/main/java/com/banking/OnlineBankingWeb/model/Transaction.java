package com.banking.OnlineBankingWeb.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "from_customer_id")
    private int fromCustomerId;

    @Column(name = "to_account")
    private String toAccount;

    @Column(name = "amount")
    private double amount;

    @Column(name = "type")
    private String type; // CREDIT, DEBIT, TRANSFER, BILL, FD

    @Column(name = "description")
    private String description;

    @Column(name = "transfer_type")
    private String transferType; // NEFT, RTGS, IMPS, UPI

    @Column(name = "status")
    private String status; // SUCCESS, FAILED, PENDING

    @Column(name = "balance_after")
    private double balanceAfter;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "remarks")
    private String remarks;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.transactionId == null) {
            this.transactionId = "TXN" + System.currentTimeMillis();
        }
        if (this.status == null) this.status = "SUCCESS";
    }

    // ── Getters & Setters ────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public int getFromCustomerId() { return fromCustomerId; }
    public void setFromCustomerId(int fromCustomerId) { this.fromCustomerId = fromCustomerId; }
    public String getToAccount() { return toAccount; }
    public void setToAccount(String toAccount) { this.toAccount = toAccount; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}