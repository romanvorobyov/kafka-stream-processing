package io.confluent.developer.livestreams.service;

import io.confluent.developer.livestreams.dto.AccountRecord;
import io.confluent.developer.livestreams.dto.HistoryRecord;

import java.util.List;
import java.util.Optional;

public interface BalanceService {
    /**
     * @param id - bank account id
     * @return falance for the account
     */
    Optional<Long> getBalance(Long id);

    /**
     * @param id     - bank account id
     * @param amount - sum of money to add to (or subtract from) the account
     */
    void changeBalance(Long id, Long amount);

    List<AccountRecord> listAccounts();

    Optional<List<HistoryRecord>> listAccountEvents(long id);
}
