package com.banking.OnlineBankingWeb.service;

import com.banking.OnlineBankingWeb.repository.SecurityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SecurityLogCleanupService {

    @Autowired
    private SecurityLogRepository securityLogRepository;

    /**
     * Runs daily at 2:00 AM — deletes security logs older than 90 days.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldSecurityLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        securityLogRepository.deleteByCreatedAtBefore(cutoff);
        System.out.println("🧹 Security log cleanup completed. Deleted logs older than: " + cutoff);
    }
}
