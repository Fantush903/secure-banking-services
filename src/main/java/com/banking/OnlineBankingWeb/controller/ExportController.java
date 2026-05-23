package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.Customer;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

@Controller
@RequestMapping("/customer")
public class ExportController {

    // ── Export Transactions as CSV ───────────────────────────
    @GetMapping("/export-transactions")
    public void exportTransactionsCSV(HttpSession session, HttpServletResponse response) throws IOException {

        if (session.getAttribute("user") == null) {
            response.sendRedirect("/login");
            return;
        }

        Customer user = (Customer) session.getAttribute("user");

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
            writer.println("# Account Number: 1001-0000-0001");
            writer.println();

            // ── Sample data (replace with DB query in production) ──
            csvPrinter.printRecord("2026-05-01", "TXN001", "Account Opening Credit", "CREDIT", "50000.00", "50000.00", "SUCCESS");
            csvPrinter.printRecord("2026-05-05", "TXN002", "Fund Transfer to 1234567890123456", "DEBIT", "5000.00", "45000.00", "SUCCESS");
            csvPrinter.printRecord("2026-05-10", "TXN003", "Electricity Bill Payment", "DEBIT", "1200.00", "43800.00", "SUCCESS");
            csvPrinter.printRecord("2026-05-15", "TXN004", "Salary Credit", "CREDIT", "30000.00", "73800.00", "SUCCESS");

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
            writer.println("# Account No: 1001-0000-0001 | Type: SAVINGS");
            writer.println("# Statement Period: " + LocalDate.now().minusMonths(1) + " to " + LocalDate.now());
            writer.println();

            csvPrinter.printRecord("2026-05-01", "Opening Balance", "", "50000.00", "50000.00");
            csvPrinter.printRecord("2026-05-05", "Fund Transfer", "5000.00", "", "45000.00");
            csvPrinter.printRecord("2026-05-10", "Electricity Bill", "1200.00", "", "43800.00");
            csvPrinter.printRecord("2026-05-15", "Salary Credit", "", "30000.00", "73800.00");
            csvPrinter.printRecord("", "Closing Balance", "", "", "73800.00");

            csvPrinter.flush();
        }
    }
}