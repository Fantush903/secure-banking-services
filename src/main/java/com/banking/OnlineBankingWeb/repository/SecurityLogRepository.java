package com.banking.OnlineBankingWeb.repository;

import com.banking.OnlineBankingWeb.model.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Integer> {
    List<SecurityLog> findByCustomerIdOrderByCreatedAtDesc(int customerId);
}
