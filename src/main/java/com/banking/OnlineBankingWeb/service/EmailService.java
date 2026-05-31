package com.banking.OnlineBankingWeb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@securebank.com}")
    private String fromEmail;

    // ─── Send OTP Email ──────────────────────────────────────
    @Async
    public void sendOTP(String toEmail, String otp, String type) {
        String subject = type.equals("2fa") ?
                "SecureBank — Login OTP" : "SecureBank — Password Reset OTP";

        String body = buildOTPEmail(toEmail, otp, type);
        sendEmail(toEmail, subject, body);
    }

    // ─── Send Welcome Email ──────────────────────────────────
    @Async
    public void sendWelcome(String toEmail, String name) {
        String subject = "Welcome to SecureBank! 🏦";
        String body = buildWelcomeEmail(name, toEmail);
        sendEmail(toEmail, subject, body);
    }

    // ─── Send Transfer Confirmation ──────────────────────────
    @Async
    public void sendTransferConfirmation(String toEmail, String name,
                                         String toAccount, double amount, String type) {
        String subject = "SecureBank — Transfer Confirmation ₹" + String.format("%.2f", amount);
        String body = buildTransferEmail(name, toAccount, amount, type);
        sendEmail(toEmail, subject, body);
    }

    // ─── Send Loan Status Email ──────────────────────────────
    @Async
    public void sendLoanStatus(String toEmail, String name,
                               String loanType, double amount, String status) {
        String subject = "SecureBank — Loan Application " + status;
        String body = buildLoanEmail(name, loanType, amount, status);
        sendEmail(toEmail, subject, body);
    }

    // ─── Core Send Method ────────────────────────────────────
    private void sendEmail(String to, String subject, String htmlBody) {
        if (mailSender == null || fromEmail == null || fromEmail.isBlank()) {
            // Fallback: print to console if mail not configured
            System.out.println("=== EMAIL TO: " + to + " ===");
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + htmlBody.replaceAll("<[^>]*>", ""));
            System.out.println("=====================================");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "SecureBank");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("✅ Email sent to: " + to);
        } catch (Exception e) {
            System.err.println("❌ Email failed: " + e.getMessage());
        }
    }

    // ─── Email Templates ─────────────────────────────────────
    private String buildOTPEmail(String email, String otp, String type) {
        String title = type.equals("2fa") ? "Login Verification" : "Password Reset";
        String desc  = type.equals("2fa") ?
                "Someone (hopefully you!) is trying to log into your SecureBank account." :
                "You requested a password reset for your SecureBank account.";
        return """
        <div style="font-family:'Segoe UI',Arial,sans-serif;background:#f0f2f8;padding:40px 0">
          <div style="max-width:520px;margin:0 auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)">
            <div style="background:linear-gradient(135deg,#1a237e,#1565c0);padding:32px;text-align:center">
              <div style="font-size:32px;margin-bottom:8px">🏦</div>
              <h1 style="color:white;font-size:22px;margin:0">SecureBank</h1>
              <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:4px 0 0">%s</p>
            </div>
            <div style="padding:32px">
              <p style="color:#333;font-size:14px;margin-bottom:16px">%s</p>
              <div style="background:#f5f6ff;border:2px dashed #c5cae9;border-radius:12px;padding:24px;text-align:center;margin:20px 0">
                <p style="color:#666;font-size:12px;margin-bottom:8px;font-weight:600;text-transform:uppercase;letter-spacing:1px">Your OTP Code</p>
                <div style="font-size:42px;font-weight:900;color:#1a237e;letter-spacing:12px">%s</div>
                <p style="color:#e53935;font-size:12px;margin-top:8px">⏱ Expires in 5 minutes</p>
              </div>
              <p style="color:#888;font-size:12px">If you didn't request this, please ignore this email or contact support immediately.</p>
            </div>
            <div style="background:#fafafa;padding:16px 32px;text-align:center;border-top:1px solid #f0f0f0">
              <p style="color:#aaa;font-size:11px">© 2026 SecureBank — Uttaranchal University BCA Project</p>
            </div>
          </div>
        </div>
        """.formatted(title, desc, otp);
    }

    private String buildWelcomeEmail(String name, String email) {
        return """
        <div style="font-family:'Segoe UI',Arial,sans-serif;background:#f0f2f8;padding:40px 0">
          <div style="max-width:520px;margin:0 auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)">
            <div style="background:linear-gradient(135deg,#1a237e,#1565c0);padding:32px;text-align:center">
              <div style="font-size:40px;margin-bottom:8px">🎉</div>
              <h1 style="color:white;font-size:22px;margin:0">Welcome to SecureBank!</h1>
            </div>
            <div style="padding:32px">
              <h2 style="color:#1a1a2e;font-size:20px;margin-bottom:16px">Hello, %s! 👋</h2>
              <p style="color:#555;font-size:14px;line-height:1.6;margin-bottom:16px">Your account has been created successfully. Here's what you can do:</p>
              <div style="background:#f5f6ff;border-radius:10px;padding:16px;margin-bottom:20px">
                <p style="margin:8px 0;font-size:13px;color:#333">💸 <strong>Fund Transfer</strong> — Send money instantly</p>
                <p style="margin:8px 0;font-size:13px;color:#333">🏠 <strong>Loans</strong> — Apply at competitive rates</p>
                <p style="margin:8px 0;font-size:13px;color:#333">📋 <strong>Statements</strong> — Download anytime</p>
                <p style="margin:8px 0;font-size:13px;color:#333">🔒 <strong>2FA Security</strong> — Always protected</p>
              </div>
              <a href="http://localhost:8080/login" style="display:block;text-align:center;background:#1a237e;color:white;padding:14px;border-radius:8px;text-decoration:none;font-weight:600;font-size:15px">Login to Your Account →</a>
            </div>
          </div>
        </div>
        """.formatted(name);
    }

    private String buildTransferEmail(String name, String toAccount, double amount, String type) {
        return """
        <div style="font-family:'Segoe UI',Arial,sans-serif;background:#f0f2f8;padding:40px 0">
          <div style="max-width:520px;margin:0 auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)">
            <div style="background:linear-gradient(135deg,#1a237e,#1565c0);padding:32px;text-align:center">
              <div style="font-size:36px">✅</div>
              <h1 style="color:white;font-size:20px;margin:8px 0 0">Transfer Successful</h1>
            </div>
            <div style="padding:32px">
              <p style="color:#333;font-size:14px;margin-bottom:20px">Hi <strong>%s</strong>, your transfer was processed successfully.</p>
              <div style="background:#f5f6ff;border-radius:12px;padding:20px;margin-bottom:20px">
                <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #e8ecff">
                  <span style="color:#888;font-size:13px">Amount</span>
                  <span style="color:#1a237e;font-weight:700;font-size:16px">₹ %.2f</span>
                </div>
                <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #e8ecff">
                  <span style="color:#888;font-size:13px">To Account</span>
                  <span style="color:#333;font-weight:600;font-size:13px">%s</span>
                </div>
                <div style="display:flex;justify-content:space-between;padding:8px 0">
                  <span style="color:#888;font-size:13px">Transfer Type</span>
                  <span style="color:#333;font-weight:600;font-size:13px">%s</span>
                </div>
              </div>
              <p style="color:#888;font-size:12px">If you did not make this transfer, contact support immediately at 1800-123-4567.</p>
            </div>
          </div>
        </div>
        """.formatted(name, amount, toAccount, type);
    }

    private String buildLoanEmail(String name, String loanType, double amount, String status) {
        String icon = status.equals("APPROVED") ? "✅" : status.equals("REJECTED") ? "❌" : "⏳";
        String color = status.equals("APPROVED") ? "#2e7d32" : status.equals("REJECTED") ? "#c62828" : "#f57f17";
        return """
        <div style="font-family:'Segoe UI',Arial,sans-serif;background:#f0f2f8;padding:40px 0">
          <div style="max-width:520px;margin:0 auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)">
            <div style="background:linear-gradient(135deg,#1a237e,#1565c0);padding:32px;text-align:center">
              <div style="font-size:36px">%s</div>
              <h1 style="color:white;font-size:20px;margin:8px 0 0">Loan Application %s</h1>
            </div>
            <div style="padding:32px">
              <p style="color:#333;font-size:14px;margin-bottom:20px">Hi <strong>%s</strong>, your loan application status has been updated.</p>
              <div style="background:#f5f6ff;border-radius:12px;padding:20px;margin-bottom:20px">
                <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #e8ecff">
                  <span style="color:#888;font-size:13px">Loan Type</span>
                  <span style="color:#333;font-weight:600">%s</span>
                </div>
                <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #e8ecff">
                  <span style="color:#888;font-size:13px">Amount</span>
                  <span style="color:#1a237e;font-weight:700">₹ %.2f</span>
                </div>
                <div style="display:flex;justify-content:space-between;padding:8px 0">
                  <span style="color:#888;font-size:13px">Status</span>
                  <span style="color:%s;font-weight:700">%s</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        """.formatted(icon, status, name, loanType, amount, color, status);
    }
}
