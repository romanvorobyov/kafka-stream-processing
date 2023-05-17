package io.confluent.developer.livestreams.web;

import io.confluent.developer.avro.Balance;
import io.confluent.developer.livestreams.dto.AccountRecord;
import io.confluent.developer.livestreams.dto.HistoryRecord;
import io.confluent.developer.livestreams.messaging.ProducerOfBalanceEvents;
import io.confluent.developer.livestreams.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
    private final BalanceService balanceService;

    private final ProducerOfBalanceEvents eventsProducer;

    private final KafkaTemplate<Long, Balance> balanceTemplate;

    @GetMapping("/{accountId}")
    public ResponseEntity<Long> getBalance(
            @PathVariable(value = "accountId") Long accountId)
            throws ResourceNotFoundException {
        Long balance = balanceService.getBalance(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for id :: " + accountId));
        return ResponseEntity.ok().body(balance);
    }

    @GetMapping("/{accountId}/change/{diff}")
    public ResponseEntity<Long> changeBalance(
            @PathVariable(value = "accountId") Long accountId, 
            @PathVariable(value = "diff") Long diff)
            throws ResourceNotFoundException {

        balanceTemplate.send("balance-avro", accountId, new Balance(accountId, diff, "CHANGE"));
        Long balanceAfter = balanceService.getBalance(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for id :: " + accountId));
        return ResponseEntity.ok().body(balanceAfter);
    }

    @GetMapping("/")
    public ResponseEntity<List<AccountRecord>> listAccounts(){
        List<AccountRecord> accounts = balanceService.listAccounts();
        return ResponseEntity.ok().body(accounts);
    }
    
    @GetMapping("/{accountId}/events")
    @Transactional(readOnly = true)
    public ResponseEntity<List<HistoryRecord>> listEvents(
            @PathVariable(value = "accountId") Long accountId)
            throws ResourceNotFoundException {
        List<HistoryRecord> events = balanceService.listAccountEvents(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for id :: " + accountId));
        return ResponseEntity.ok().body(events);
    }

    @GetMapping("/generate")
    public void runKafkaGenerator(){
        eventsProducer.runGeneratorThreads();
    }
}
