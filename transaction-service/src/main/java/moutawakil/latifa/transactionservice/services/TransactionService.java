package moutawakil.latifa.transactionservice.services;

import moutawakil.latifa.transactionservice.entities.Transaction;
import moutawakil.latifa.transactionservice.feign.AccountRestClient;
import moutawakil.latifa.transactionservice.model.Account;
import moutawakil.latifa.transactionservice.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRestClient accountRestClient;
    private final BigDecimal maxAmount;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRestClient accountRestClient,
                              @Value("${bank.transaction.max-amount:1000}") BigDecimal maxAmount) {
        this.transactionRepository = transactionRepository;
        this.accountRestClient = accountRestClient;
        this.maxAmount = maxAmount;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction getTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }

    public List<Transaction> getTransactionsByAccount(UUID accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public Transaction createTransaction(UUID accountId, BigDecimal amount, String currency,
                                         String merchantCategory, String country) {
        checkAmount(amount);
        Account account = accountRestClient.getAccountById(accountId);
        checkDebit(account, amount, currency);

        Transaction transaction = Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .currency(currency.toUpperCase())
                .merchantCategory(merchantCategory)
                .country(country)
                .createdAt(Instant.now())
                .build();
        Transaction saved = transactionRepository.save(transaction);

        // si l'appel échoue, l'exception annule l'enregistrement (rollback)
        accountRestClient.updateBalance(accountId, -amount.doubleValue());
        return saved;
    }

    public Transaction transfer(UUID sourceId, UUID targetId, BigDecimal amount, String currency) {
        if (sourceId.equals(targetId)) {
            throw new RuntimeException("Source and target accounts must be different");
        }
        checkAmount(amount);
        Account source = accountRestClient.getAccountById(sourceId);
        Account target = accountRestClient.getAccountById(targetId);
        checkDebit(source, amount, currency);
        if (!"ACTIVE".equals(target.status()) || !target.currency().equalsIgnoreCase(currency)) {
            throw new RuntimeException("Target account must be ACTIVE and use currency " + currency);
        }

        Transaction saved = transactionRepository.save(Transaction.builder()
                .accountId(sourceId)
                .amount(amount)
                .currency(currency.toUpperCase())
                .merchantCategory("TRANSFER")
                .createdAt(Instant.now())
                .build());

        accountRestClient.updateBalance(sourceId, -amount.doubleValue());
        try {
            accountRestClient.updateBalance(targetId, amount.doubleValue());
        } catch (RuntimeException e) {
            accountRestClient.updateBalance(sourceId, amount.doubleValue()); // compensation
            throw e;
        }
        return saved;
    }

    private void checkAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new RuntimeException("Amount must be strictly positive");
        }
        if (amount.compareTo(maxAmount) > 0) {
            throw new RuntimeException("Amount exceeds the maximum allowed: " + maxAmount);
        }
    }

    private void checkDebit(Account account, BigDecimal amount, String currency) {
        if (!"ACTIVE".equals(account.status())) {
            throw new RuntimeException("Account is " + account.status());
        }
        if (!account.currency().equalsIgnoreCase(currency)) {
            throw new RuntimeException("Currency does not match the account currency " + account.currency());
        }
        if (BigDecimal.valueOf(account.balance()).compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
    }
}
