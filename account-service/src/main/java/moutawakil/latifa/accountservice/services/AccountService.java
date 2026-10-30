package moutawakil.latifa.accountservice.services;

import jakarta.transaction.Transactional;
import moutawakil.latifa.accountservice.entities.Account;
import moutawakil.latifa.accountservice.enums.AccountStatus;
import moutawakil.latifa.accountservice.repositories.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id));
    }

    public Account createAccount(String customerReference, String currency, double initialBalance) {
        Account account = Account.builder()
                .customerReference(customerReference)
                .currency(currency.toUpperCase())
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .build();
        return accountRepository.save(account);
    }

    public Account updateStatus(UUID id, AccountStatus status) {
        Account account = getAccountById(id);
        account.setStatus(status);
        return accountRepository.save(account);
    }

    // delta négatif = débit, positif = crédit
    public Account updateBalance(UUID id, double delta) {
        Account account = getAccountById(id);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is " + account.getStatus());
        }
        double newBalance = account.getBalance() + delta;
        if (newBalance < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        account.setBalance(newBalance);
        return accountRepository.save(account);
    }
}

