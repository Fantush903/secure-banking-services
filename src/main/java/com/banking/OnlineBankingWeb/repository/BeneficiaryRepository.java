package com.banking.OnlineBankingWeb.repository;

import com.banking.OnlineBankingWeb.model.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Integer> {
    List<Beneficiary> findByCustomerId(int customerId);
    void deleteByIdAndCustomerId(int id, int customerId);
}