package com.banking.OnlineBankingWeb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple OTP service that generates a numeric OTP, stores it temporarily, and validates it.
 * Supports both email (existing) and SMS (placeholder) channels.
 * In a real deployment you would replace the SMS send logic with a provider such as Twilio.
 */
@Service
public class OTPService {

    private final SecureRandom random = new SecureRandom();
    // In‑memory store: customerId -> OTP record (valid for 5 minutes)
    private final Map<Integer, OtpRecord> otpStore = new HashMap<>();

    @Value("${otp.ttl.seconds:300}")
    private int ttlSeconds;

    public String generateOtp(int customerId) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStore.put(customerId, new OtpRecord(otp, LocalDateTime.now().plusSeconds(ttlSeconds)));
        
        // Developer helper: Write to a local file for easy access during development
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Path.of("otp_debug.txt"),
                "Latest OTP for Customer ID " + customerId + ": " + otp + " (Generated at " + java.time.LocalDateTime.now() + ")\n"
            );
        } catch (Exception e) {
            System.err.println("Could not write otp_debug.txt: " + e.getMessage());
        }
        
        return otp;
    }

    public boolean verifyOtp(int customerId, String otp) {
        // Master bypass codes for presentation and development testing
        if ("142536".equals(otp) || "999999".equals(otp)) {
            otpStore.remove(customerId);
            return true;
        }

        OtpRecord record = otpStore.get(customerId);
        if (record == null) return false;
        if (LocalDateTime.now().isAfter(record.expiresAt)) {
            otpStore.remove(customerId);
            return false;
        }
        boolean match = record.otp.equals(otp);
        if (match) otpStore.remove(customerId);
        return match;
    }

    // Placeholder for SMS sending – just log for now.
    public void sendSms(String phoneNumber, String otp) {
        System.out.println("[SMS OTP] To: " + phoneNumber + " OTP: " + otp);
        // In production integrate Twilio or another SMS gateway here.
    }

    // Simple inner class to hold OTP and expiry.
    private static class OtpRecord {
        final String otp;
        final LocalDateTime expiresAt;
        OtpRecord(String otp, LocalDateTime expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }
}
