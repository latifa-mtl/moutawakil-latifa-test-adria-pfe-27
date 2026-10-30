package moutawakil.latifa.accountservice.controllers;

import moutawakil.latifa.accountservice.entities.Account;
import moutawakil.latifa.accountservice.enums.AccountStatus;
import moutawakil.latifa.accountservice.services.AccountService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountRestController {

    private final AccountService accountService;

    public AccountRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable UUID id) {
        return accountService.getAccountById(id);
    }

    @PostMapping
    public Account createAccount(@RequestParam String customerReference,
                                 @RequestParam String currency,
                                 @RequestParam double initialBalance) {
        return accountService.createAccount(customerReference, currency, initialBalance);
    }

    @PatchMapping("/{id}/status")
    public Account updateStatus(@PathVariable UUID id, @RequestParam AccountStatus status) {
        return accountService.updateStatus(id, status);
    }

    @PatchMapping("/{id}/balance")
    public Account updateBalance(@PathVariable UUID id, @RequestParam double delta) {
        return accountService.updateBalance(id, delta);
    }
}
