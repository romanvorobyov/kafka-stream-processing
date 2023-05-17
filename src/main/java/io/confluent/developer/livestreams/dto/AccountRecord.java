package io.confluent.developer.livestreams.dto;

import java.time.LocalDateTime;

public record AccountRecord (long id, long value, short version,
                             //@JsonSerialize(using = LocalDateTimeSerializer.class)
                             LocalDateTime created,
                             //@JsonSerialize(using = LocalDateTimeSerializer.class)
                             LocalDateTime updated
){
}
