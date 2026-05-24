package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.*;
import com.banking.OnlineBankingWeb.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/customer")
public class ExportController {

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private AccountRepository accountRepository;

    // ── Export Transactions as CSV ───────────────────────────
    @GetMapping("/export-transactions")
    public void exportTransactionsCSV(HttpSession session, HttpServletResponse response) throws IOException {

        if (session.getAttribute("user") == null) {
            response.sendRedirect("/login");
            return;
        }

        Customer user = (Customer) session.getAttribute("user");
        Account account = accountRepository.findByCustomerId(user.getCustomerID());

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"transactions_" + user.getName().replaceAll("\\s+", "_") +
                        "_" + LocalDate.now() + ".csv\"");

        try (PrintWriter writer = response.getWriter();
             CSVPrinter csvPrinter = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader("Date", "Transaction ID", "Description", "Type", "Amount (₹)", "Balance (₹)", "Status")
                             .build())) {

            // ── Write account info header ────────────────────
            writer.println("# SecureBank — Transaction Statement");
            writer.println("# Account Holder: " + user.getName());
            writer.println("# Email: " + user.getEmail());
            writer.println("# Generated: " + LocalDate.now());
            writer.println("# Account Number: " + (account != null ? account.getAccountNumber() : "N/A"));
            writer.println();

            // ── Retrieve real transactions from DB ──
            List<Transaction> transactions = transactionRepository.findByFromCustomerIdOrderByCreatedAtDesc(user.getCustomerID());
            for (Transaction tx : transactions) {
                csvPrinter.printRecord(
                        tx.getCreatedAt().toLocalDate().toString(),
                        tx.getTransactionId(),
                        tx.getDescription(),
                        tx.getType(),
                        String.format("%.2f", tx.getAmount()),
                        String.format("%.2f", tx.getBalanceAfter()),
                        tx.getStatus()
                );
            }

            csvPrinter.flush();
        }
    }

    // ── Export Statement as CSV ──────────────────────────────
    @GetMapping("/export-statement")
    public void exportStatementCSV(HttpSession session, HttpServletResponse response) throws IOException {
        if (session.getAttribute("user") == null) {
            response.sendRedirect("/login");
            return;
        }
        Customer user = (Customer) session.getAttribute("user");
        Account account = accountRepository.findByCustomerId(user.getCustomerID());

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"statement_" + LocalDate.now() + ".csv\"");

        try (PrintWriter writer = response.getWriter();
             CSVPrinter csvPrinter = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader("Date", "Particulars", "Debit (₹)", "Credit (₹)", "Balance (₹)")
                             .build())) {

            writer.println("# SecureBank — Account Statement");
            writer.println("# Account Holder: " + user.getName());
            writer.println("# Account No: " + (account != null ? account.getAccountNumber() : "N/A") + " | Type: " + (account != null ? account.getAccountType() : "SAVINGS"));
            writer.println("# Statement Period: " + LocalDate.now().minusMonths(1) + " to " + LocalDate.now());
            writer.println();

            // ── Retrieve real transactions from DB ──
            List<Transaction> transactions = transactionRepository.findByFromCustomerIdOrderByCreatedAtDesc(user.getCustomerID());
            for (Transaction tx : transactions) {
                String debit = "DEBIT".equals(tx.getType()) ? String.format("%.2f", tx.getAmount()) : "";
                String credit = "CREDIT".equals(tx.getType()) ? String.format("%.2f", tx.getAmount()) : "";
                csvPrinter.printRecord(
                        tx.getCreatedAt().toLocalDate().toString(),
                        tx.getDescription(),
                        debit,
                        credit,
                        String.format("%.2f", tx.getBalanceAfter())
                );
            }

            csvPrinter.flush();
        }
    }
}