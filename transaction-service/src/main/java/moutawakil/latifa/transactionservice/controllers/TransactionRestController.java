package moutawakil.latifa.transactionservice.web.rest;

import moutawakil.latifa.transactionservice.entities.Transaction;
import moutawakil.latifa.transactionservice.services.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionRestController {

    private final TransactionService transactionService;

    public TransactionRestController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<Transaction> getTransactions(@RequestParam(required = false) UUID accountId) {
        return accountId == null
                ? transactionService.getAllTransactions()
                : transactionService.getTransactionsByAccount(accountId);
    }

    @GetMapping("/{id}")
    public Transaction getTransactionById(@PathVariable UUID id) {
        return transactionService.getTransactionById(id);
    }

    @PostMapping
    public Transaction createTransaction(@RequestParam UUID accountId,
                                         @RequestParam BigDecimal amount,
                                         @RequestParam String currency,
                                         @RequestParam(required = false) String merchantCategory,
                                         @RequestParam(required = false) String country) {
        return transactionService.createTransaction(accountId, amount, currency, merchantCategory, country);
    }

    @PostMapping("/transfers")
    public Transaction transfer(@RequestParam UUID sourceAccountId,
                                @RequestParam UUID targetAccountId,
                                @RequestParam BigDecimal amount,
                                @RequestParam String currency) {
        return transactionService.transfer(sourceAccountId, targetAccountId, amount, currency);
    }
}