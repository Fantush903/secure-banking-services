package com.banking.OnlineBankingWeb.repository;

import com.banking.OnlineBankingWeb.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByFromCustomerIdOrderByCreatedAtDesc(int customerId);
    List<Transaction> findTop5ByFromCustomerIdOrderByCreatedAtDesc(int customerId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.fromCustomerId = ?1 AND t.type = 'DEBIT'")
    Double sumDebitByCustomerId(int customerId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.fromCustomerId = ?1 AND t.type = 'CREDIT'")
    Double sumCreditByCustomerId(int customerId);

    List<Transaction> findAllByOrderByCreatedAtDesc();
}