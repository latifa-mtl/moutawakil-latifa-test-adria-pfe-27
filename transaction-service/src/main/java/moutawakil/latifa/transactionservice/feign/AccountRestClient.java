package moutawakil.latifa.transactionservice.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import moutawakil.latifa.transactionservice.model.Account;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "account-service", url = "${bank.account-service.url:}")
public interface AccountRestClient {

    @GetMapping("/accounts/{id}")
    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountFallback")
    Account getAccountById(@PathVariable("id") UUID id);

    @PatchMapping("/accounts/{id}/balance")
    @CircuitBreaker(name = "accountService", fallbackMethod = "updateBalanceFallback")
    Account updateBalance(@PathVariable("id") UUID id, @RequestParam("delta") double delta);

    default Account getAccountFallback(UUID id, Exception e) {
        throw new RuntimeException("account-service unavailable: " + e.getMessage());
    }

    default Account updateBalanceFallback(UUID id, double delta, Exception e) {
        throw new RuntimeException("account-service unavailable: " + e.getMessage());
    }
}
