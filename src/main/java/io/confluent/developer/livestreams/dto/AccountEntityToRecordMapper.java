package io.confluent.developer.livestreams.dto;

import io.confluent.developer.livestreams.model.Account;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountEntityToRecordMapper {
    AccountRecord entityToRecord(Account account);
    List<AccountRecord> toListOfDto(List<Account> accounts);
}
