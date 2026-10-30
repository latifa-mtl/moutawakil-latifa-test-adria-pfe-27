package moutawakil.latifa.transactionservice.controllers;

import moutawakil.latifa.transactionservice.entities.Transaction;
import moutawakil.latifa.transactionservice.services.TransactionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
public class TransactionGraphQLController {

    private final TransactionService transactionService;

    public TransactionGraphQLController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.getAllTransactions();
    }

    @QueryMapping
    public Transaction transactionById(@Argument UUID id) {
        return transactionService.getTransactionById(id);
    }

    @QueryMapping
    public List<Transaction> transactionsByAccount(@Argument UUID accountId) {
        return transactionService.getTransactionsByAccount(accountId);
    }

    @MutationMapping
    public Transaction createTransaction(@Argument UUID accountId, @Argument BigDecimal amount,
                                         @Argument String currency, @Argument String merchantCategory,
                                         @Argument String country) {
        return transactionService.createTransaction(accountId, amount, currency, merchantCategory, country);
    }

    @MutationMapping
    public Transaction transfer(@Argument UUID sourceAccountId, @Argument UUID targetAccountId,
                                @Argument BigDecimal amount, @Argument String currency) {
        return transactionService.transfer(sourceAccountId, targetAccountId, amount, currency);
    }
}
