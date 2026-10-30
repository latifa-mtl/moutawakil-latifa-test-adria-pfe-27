package moutawakil.latifa.accountservice;

import moutawakil.latifa.accountservice.entities.Account;
import moutawakil.latifa.accountservice.enums.AccountStatus;
import moutawakil.latifa.accountservice.repositories.AccountRepository;
import moutawakil.latifa.accountservice.services.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner start(AccountRepository accountRepository, AccountService accountService) {
        return args -> {
            // insertion
            accountRepository.save(Account.builder().customerReference("001")
                    .status(AccountStatus.ACTIVE).currency("DH").balance(5000).build());
            accountRepository.save(Account.builder().customerReference("002")
                    .status(AccountStatus.ACTIVE).currency("DH").balance(1200).build());
            accountRepository.save(Account.builder().customerReference("003")
                    .status(AccountStatus.BLOCKED).currency("DH").balance(300).build());
            accountRepository.save(Account.builder().customerReference("004")
                    .status(AccountStatus.ACTIVE).currency("DH").balance(800).build());

            // consultation des méthodes du service
            accountService.getAllAccounts().forEach(a ->
                    System.out.println(a.getId() + " | " + a.getCustomerReference() + " | "
                            + a.getStatus() + " | " + a.getCurrency() + " | " + a.getBalance()));
        };
    }

}
