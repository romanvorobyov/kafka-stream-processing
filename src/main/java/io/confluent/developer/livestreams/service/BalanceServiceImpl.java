package io.confluent.developer.livestreams.service;

import io.confluent.developer.livestreams.cache.MyCache;
import io.confluent.developer.livestreams.dto.AccountEntityToRecordMapper;
import io.confluent.developer.livestreams.dto.AccountRecord;
import io.confluent.developer.livestreams.dto.HistoryEntityToRecordMapper;
import io.confluent.developer.livestreams.dto.HistoryRecord;
import io.confluent.developer.livestreams.model.Account;
import io.confluent.developer.livestreams.model.History;
import io.confluent.developer.livestreams.repository.AccountRepository;
import io.confluent.developer.livestreams.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Slf4j
@Service
@RequiredArgsConstructor
class BalanceServiceImpl implements BalanceService {

    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private final HistoryRepository historyRepository;

    @Autowired
    private final AccountEntityToRecordMapper accountMapper;

    @Autowired
    private final HistoryEntityToRecordMapper historyMapper;

    @Autowired
    private final MyCache cache;

    @Override
    @Transactional(readOnly = true)
    public List<AccountRecord> listAccounts() {
        List<Account> accounts = accountRepository.findAll();
        return accountMapper.toListOfDto(accounts);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<HistoryRecord>> listAccountEvents(long id) {
        List<History> events = historyRepository.findByAccountId(id);
        return Optional.of(historyMapper.toListOfDto(events));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> getBalance(Long id) {
        String timeStr = LocalDateTime.now().format(ISO_LOCAL_DATE_TIME);

        //TODO get from distributed cache (Redis), not from one-node concurrent map
        Optional<Long> valueCacheOpt = Optional.ofNullable(cache.getAccounts().get(id));
        if (valueCacheOpt.isEmpty()) {
            long[] valueDb = new long[1];
            accountRepository.findById(id).ifPresentOrElse(
                    acc -> { //update cache
                        valueDb[0] = acc.getValue();
                        cache.getAccounts().put(id, acc.getValue());
                    }, () -> valueDb[0] = -1);
            log.debug("[GET from DB ({}) = {}] at {}", id, valueDb[0], timeStr);
            return Optional.ofNullable((valueDb[0] != -1) ? valueDb[0] : null);
        } else {
            lookInDbToCheckIfCacheDiffers(valueCacheOpt.get(), accountRepository, id); //TODO remove this call
            log.debug("[GET from Cache({}) = {}] at {}", id, valueCacheOpt.orElse(-1L), timeStr);
            return valueCacheOpt;
        }
    }

    @Override
    @Transactional
    public void changeBalance(Long id, Long diff) {

        accountRepository.findById(id).ifPresentOrElse(account -> {
                    final long newValueCache = cache.getAccounts().compute(id, (k, v) -> v != null ? v + diff : diff);
                    long newValueDb = account.getValue() + diff;
                    account.setValue(newValueDb); //not from cache
                    accountRepository.saveAndFlush(account);
                    if (newValueDb != newValueCache) {
                        log.error("ALERT change() !!!: newValueDb <> newValueCache, {}, {}", newValueDb, newValueCache);
                    }

                    log.info("[UPDATE ({},{}) = {}] at {}", id, diff, account.getValue(),
                            getDateTimeFmt(account.getUpdated()));
                    saveHistory(diff, account);
                },
                () -> {
                    Account account = Account.builder().id(id).value(diff).build();
                    accountRepository.saveAndFlush(account);
                    log.info("[INSERT ({}) = {}] at {}", id, diff,
                            getDateTimeFmt(account.getCreated()));
                    saveHistory(diff, account);
                });
    }

    private void saveHistory(Long diff, Account account) {
        History history = History.builder()
                .account(account)
                .diff(diff)
                .value(account.getValue())
                .build();
        historyRepository.saveAndFlush(history);
        log.info("[HIST id={} : ({}, {})] at {}", history.getId(), account.getId(), diff,
                getDateTimeFmt(history.getEventTime()));
    }

    private static String getDateTimeFmt(LocalDateTime time) {
        return (time != null) ? time.format(ISO_LOCAL_DATE_TIME) : "[time unknown: NOT-SAVED-YET] ";
    }

    private static void lookInDbToCheckIfCacheDiffers(long valueCache, AccountRepository accountRepository, long id) {
        long[] valueDb = new long[1];
        accountRepository.findById(id).ifPresentOrElse(
                acc -> valueDb[0] = acc.getValue(), () -> valueDb[0] = -1);
        if (valueDb[0] == -1) {
            log.debug("value is not in DB");
            return;
        }
        if (valueDb[0] != valueCache) {
            log.error("ALERT get() !!!: valueDb <> valueCache, {}, {}", valueDb, valueCache);
        }
    }
}
