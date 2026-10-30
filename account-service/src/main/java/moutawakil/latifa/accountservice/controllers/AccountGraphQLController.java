package moutawakil.latifa.accountservice.controllers;

import moutawakil.latifa.accountservice.entities.Account;
import moutawakil.latifa.accountservice.enums.AccountStatus;
import moutawakil.latifa.accountservice.services.AccountService;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class AccountGraphQLController {

    private final AccountService accountService;

    public AccountGraphQLController(AccountService accountService) {
        this.accountService = accountService;
    }

    @QueryMapping
    public List<Account> accounts() {
        return accountService.getAllAccounts();
    }

    @QueryMapping
    public Account accountById(@Argument UUID id) {
        return accountService.getAccountById(id);
    }

    @MutationMapping
    public Account createAccount(@Argument String customerReference,
                                 @Argument String currency,
                                 @Argument double initialBalance) {
        return accountService.createAccount(customerReference, currency, initialBalance);
    }

    @MutationMapping
    public Account updateAccountStatus(@Argument UUID id, @Argument AccountStatus status) {
        return accountService.updateStatus(id, status);
    }

    @MutationMapping
    public Account updateAccountBalance(@Argument UUID id, @Argument double delta) {
        return accountService.updateBalance(id, delta);
    }
}
