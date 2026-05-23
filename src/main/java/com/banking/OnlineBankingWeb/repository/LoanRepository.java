package com.banking.OnlineBankingWeb.repository;

import com.banking.OnlineBankingWeb.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Integer> {
    List<Loan> findByCustomerIdOrderByAppliedAtDesc(int customerId);
    List<Loan> findByStatusOrderByAppliedAtDesc(String status);
    long countByStatus(String status);
}