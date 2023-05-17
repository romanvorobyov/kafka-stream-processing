package io.confluent.developer.livestreams.dto;

import java.time.LocalDateTime;

public record HistoryRecord(long id, long accountId, long value, long diff,
                            //@JsonSerialize(using = LocalDateTimeSerializer.class)
                            LocalDateTime eventTime) {
}
