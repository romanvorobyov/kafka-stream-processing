package io.confluent.developer.livestreams.messaging;

import io.confluent.developer.avro.Balance;
import io.confluent.developer.livestreams.service.BalanceService;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessorOfBalanceEvents {

    private final BalanceService balanceService;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String srUrl;

    @Value("${spring.kafka.properties.basic.auth.credentials.source}")
    private String crSource;

    @Value("${spring.kafka.properties.basic.auth.user.info}")
    private String authUser;

    @Autowired
    public void process(StreamsBuilder builder) {

        final Serde<Long> keySerde = Serdes.Long();
        final KStream<Long, Balance> requestsStream = builder.stream("balance-avro", Consumed.with(keySerde, specificAvro()));

        requestsStream.mapValues(
            avro -> {
                String timeStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                if (avro.getAction().equalsIgnoreCase("OTHER")){
                    return null;
                }
                if (avro.getAction().equalsIgnoreCase("GET")){
                    var balance = balanceService.getBalance(avro.getId()).orElse(-1L);
                    log.info("[GET ({}) = {}] at {}", avro.getId(), balance, timeStr);
                }
                else {
                    balanceService.changeBalance(avro.getId(), avro.getDiff());
                    getAndLogResult(avro, timeStr);
                }
                return null;
            });
    }

    private void getAndLogResult(Balance avro, String timeStr) {
        var newValue = balanceService.getBalance(avro.getId()).orElse(-1L);
        if (newValue == -1){
            log.error("CHANGE({},{}) not applied - account not found (or balance == -1)", avro.getId(), avro.getDiff());
        }
        if (newValue == avro.getDiff()) {
            log.info("CHANGE [NEW balance({}) = {}] at {}", avro.getId(), newValue, timeStr);
        } else {
            log.info("CHANGE [CHANGE({},{}) = {}] at {}", avro.getId(), avro.getDiff(), newValue, timeStr);
        }
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
