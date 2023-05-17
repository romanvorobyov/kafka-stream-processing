package io.confluent.developer.livestreams.dto;

import io.confluent.developer.livestreams.model.History;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HistoryEntityToRecordMapper {
    @Mapping(target="accountId", source="account.id")
    HistoryRecord entityToRecord(History event);

    List<HistoryRecord> toListOfDto(List<History> events);
}
