package moutawakil.latifa.transactionservice;

import moutawakil.latifa.transactionservice.entities.Transaction;
import moutawakil.latifa.transactionservice.repositories.TransactionRepository;
import moutawakil.latifa.transactionservice.services.TransactionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@SpringBootApplication
@EnableFeignClients
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner start(TransactionRepository transactionRepository, TransactionService transactionService) {
        return args -> {
            UUID account1 = UUID.randomUUID();
            UUID account2 = UUID.randomUUID();

            transactionRepository.save(Transaction.builder().accountId(account1)
                    .amount(new BigDecimal("120.50")).currency("DH")
                    .merchantCategory("GROCERY").country("MAR").createdAt(Instant.now()).build());
            transactionRepository.save(Transaction.builder().accountId(account1)
                    .amount(new BigDecimal("45.00")).currency("DH")
                    .merchantCategory("FUEL").country("MAR").createdAt(Instant.now()).build());
            transactionRepository.save(Transaction.builder().accountId(account2)
                    .amount(new BigDecimal("300.00")).currency("DH")
                    .merchantCategory("ELECTRONICS").country("MAR").createdAt(Instant.now()).build());

            transactionService.getAllTransactions().forEach(t ->
                    System.out.println(t.getId() + " | " + t.getAccountId() + " | " + t.getAmount()
                            + " " + t.getCurrency() + " | " + t.getMerchantCategory() + " | " + t.getCreatedAt()));
            System.out.println("account1 = " + account1);
            System.out.println("account2 = " + account2);
        };
    }
}
