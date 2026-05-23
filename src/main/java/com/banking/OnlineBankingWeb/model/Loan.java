package com.banking.OnlineBankingWeb.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_application")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "customer_id")
    private int customerId;

    @Column(name = "loan_type")
    private String loanType; // HOME, PERSONAL, EDUCATION, CAR

    @Column(name = "amount")
    private double amount;

    @Column(name = "tenure_months")
    private int tenureMonths;

    @Column(name = "interest_rate")
    private double interestRate;

    @Column(name = "emi_amount")
    private double emiAmount;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "monthly_income")
    private double monthlyIncome;

    @Column(name = "status")
    private String status; // PENDING, APPROVED, REJECTED, ACTIVE, CLOSED

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "admin_remarks")
    private String adminRemarks;

    @PrePersist
    public void prePersist() {
        this.appliedAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    // ── Getters & Setters ────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }
    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public double getEmiAmount() { return emiAmount; }
    public void setEmiAmount(double emiAmount) { this.emiAmount = emiAmount; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(double monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }
}