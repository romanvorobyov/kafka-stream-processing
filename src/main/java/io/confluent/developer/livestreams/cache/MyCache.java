package io.confluent.developer.livestreams.cache;

import io.confluent.developer.avro.Balance;
import io.confluent.developer.livestreams.repository.AccountRepository;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@Getter
public class MyCache {
    private final ConcurrentHashMap<Long, Long> accounts = new ConcurrentHashMap<>();

    private final AccountRepository accountRepository;

    @Value("${writeIdList}")
    private String writeIdListStr;

    @Value("${readIdList}")
    private String readIdListStr;

    @Value("${spring.kafka.properties.schema.registry.url}")
    String srUrl;

    @Value("${spring.kafka.properties.basic.auth.credentials.source}")
    String crSource;

    @Value("${spring.kafka.properties.basic.auth.user.info}")
    String authUser;

    @EventListener(ApplicationStartedEvent.class)
    private int warmCacheFromDb(){
        final List<Long> readIdsList = Arrays.stream(readIdListStr
                        .replaceAll(" ", "").split(","))
                .map(Long::parseLong).toList();
        final List<Long> writeIdsList = Arrays.stream(writeIdListStr
                        .replaceAll(" ", "").split(","))
                .map(Long::parseLong).toList();
        var allIdsSet = new HashSet<>(readIdsList);
        allIdsSet.addAll(writeIdsList);
        allIdsSet.forEach(id-> accountRepository.findById(id).ifPresent(
                acc -> accounts.put(id,acc.getValue())));
        log.info("------------------");
        log.info("WARMED cache from DB: {} records", accounts.size());
        log.info("------------------");
        return accounts.size();
    }

    //@EventListener(ApplicationStartedEvent.class) //Another option of cache-warming
    //@Autowired
    private int warmCacheFromKafkaTopicWithAllChangeEvents(StreamsBuilder builder){
        final List<Long> readIdsList = Arrays.stream(readIdListStr
                        .replaceAll(" ", "").split(","))
                .map(Long::parseLong).toList();
        final List<Long> writeIdsList = Arrays.stream(writeIdListStr
                        .replaceAll(" ", "").split(","))
                .map(Long::parseLong).toList();
        var allIdsSet = new HashSet<>(readIdsList);
        allIdsSet.addAll(writeIdsList);

        final Serde<Long> keySerde = Serdes.Long();
        final KStream<Long, Balance> requestsStream = builder.stream("balance-avro",
                Consumed.with(keySerde, specificAvro())
                        .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST));

        log.info("----------------------------------  ->");
        requestsStream.mapValues(
                avro -> {
                    final long id = avro.getId();
                    if (allIdsSet.contains(id) && avro.getAction().equalsIgnoreCase("CHANGE")){
                        final long diff = avro.getDiff();
                        Long newValue = accounts.compute(id, (k, v) -> v != null ? v + diff : diff);
                        if (newValue == diff) {
                            log.debug("WARMING cache [NEW ({}) = {}]", id, newValue);
                        } else {
                            log.debug("WARMING CACHE [CHANGE({},{}) = {}]", id, diff, newValue);
                        }
                    }
                    return null;
                });
        log.info("WARMED cache from Kafka from the beginning of the topic: {} records", accounts.size());
        log.info("<- ------------------------------------");
        return accounts.size();
    }
    private SpecificAvroSerde<Balance> specificAvro() {
        SpecificAvroSerde<Balance> serde = new SpecificAvroSerde<>();
        final Map<String, String>
                config =
                Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, srUrl,
                        "basic.auth.credentials.source", crSource,
                        AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, authUser);
        serde.configure(config, false);
        return serde;
    }
}
