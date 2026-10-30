package moutawakil.latifa.transactionservice.repositories;

import moutawakil.latifa.transactionservice.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
