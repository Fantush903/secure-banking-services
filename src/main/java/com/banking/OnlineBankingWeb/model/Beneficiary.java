package com.banking.OnlineBankingWeb.model;

import jakarta.persistence.*;

@Entity
@Table(name = "beneficiary")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "customer_id")
    private int customerId;

    @Column(name = "name")
    private String name;

    @Column(name = "account_no")
    private String accountNo;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "ifsc")
    private String ifsc;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "mobile")
    private String mobile;

    // ── Getters & Setters ────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
}