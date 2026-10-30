package moutawakil.latifa.transactionservice.feign;

import moutawakil.latifa.transactionservice.model.Account;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "account-service",
        fallbackFactory = AccountRestClient.Fallback.class)
public interface AccountRestClient {

    @GetMapping("/accounts/{id}")
    Account getAccountById(@PathVariable("id") UUID id);

    @PatchMapping("/accounts/{id}/balance")
    Account updateBalance(@PathVariable("id") UUID id, @RequestParam("delta") double delta);

    @Component
    class Fallback implements FallbackFactory<AccountRestClient> {
        @Override
        public AccountRestClient create(Throwable cause) {
            return new AccountRestClient() {
                @Override
                public Account getAccountById(UUID id) {
                    throw new RuntimeException("account-service unavailable: " + cause.getMessage());
                }

                @Override
                public Account updateBalance(UUID id, double delta) {
                    throw new RuntimeException("account-service unavailable: " + cause.getMessage());
                }
            };
        }
    }
}
