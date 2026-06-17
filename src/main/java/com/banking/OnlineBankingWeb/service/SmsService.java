package com.banking.OnlineBankingWeb.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    /**
     * Simulates sending an SMS asynchronously.
     * For production, integrate with Twilio or AWS SNS here.
     */
    @Async
    public void sendSmsAsync(String phoneNumber, String otp, String type) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            System.out.println("❌ Cannot send SMS: No phone number provided.");
            return;
        }

        System.out.println("==================================================");
        System.out.println("📱 MOCK SMS GATEWAY");
        System.out.println("To: " + phoneNumber);
        
        String message;
        if ("reset".equals(type)) {
            message = "Your SecureBank Password Reset OTP is: " + otp + ". Do not share this with anyone.";
        } else {
            message = "Your SecureBank Login OTP is: " + otp + ". Do not share this with anyone.";
        }
        
        System.out.println("Message: " + message);
        System.out.println("==================================================");
        
        // Simulate network delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
