package com.rikkeibank.transaction.client;

import com.rikkeibank.transaction.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @PostMapping("/api/v1/accounts/debit")
    BalanceOperationResponse debit(@RequestBody DebitRequest request);

    @PostMapping("/api/v1/accounts/credit")
    BalanceOperationResponse credit(
            @RequestBody CreditRequest request,
            @RequestHeader(value = "X-Simulate-Error", required = false) String simulateError);

    @PostMapping("/api/v1/accounts/compensate-debit")
    BalanceOperationResponse compensateDebit(@RequestBody CompensateDebitRequest request);
}
