package io.confluent.developer.livestreams.messaging;

import io.confluent.developer.avro.Balance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProducerOfBalanceEvents {

    @Value("${writeIdList}")
    private String writeIdListStr;

    @Value("${readIdList}")
    private String readIdListStr;

    @Value("${threadCount}")
    private Integer threadCount;

    @Value("${readQuota}")
    private Integer readQuota;

    @Value("${writeQuota}")
    private Integer writeQuota;


    @Value("${requestsRateInOneThread:1000}")
    private Long requestsRateInOneThread;
    @Value("${threadStartDelayAfterPrevious:100}")
    private Long threadStartDelayAfterPrevious;

    private final KafkaTemplate<Long, Balance> balanceTemplate;

    private final ConcurrentHashMap<Long, Long> producerMap = new ConcurrentHashMap<>();

    //    @EventListener(ApplicationStartedEvent.class) //TODO return auto-run
    public void runGeneratorThreads() {
        final List<Integer> readIdsList = Arrays.stream(readIdListStr
                        .replaceAll(" ", "").split(","))
                .map(Integer::parseInt).toList();
        final List<Integer> writeIdsList = Arrays.stream(writeIdListStr
                        .replaceAll(" ", "").split(","))
                .map(Integer::parseInt).toList();

        try (var generator = Executors.newFixedThreadPool(threadCount)) {
            long[] delay = new long[1];
            for (long i = 0; i < threadCount; i++) {
                delay[0] = threadStartDelayAfterPrevious * i;
                generator.execute(() -> generatorThread(readIdsList, writeIdsList, delay[0], requestsRateInOneThread));
            }
        }
    }

    private void generatorThread(List<Integer> readIdsList, List<Integer> writeIdsList, long startDelay, long period) {
        Random random = new Random();

        final Flux<Balance> balanceEvents = Flux.fromStream(Stream.generate(() ->
        {
            long id;
            int actionType = random.nextInt(100);
            String action;
            long[] diff = new long[1];
            if (actionType < readQuota) {
                id = readIdsList.get(random.nextInt(readIdsList.size()));
                action = "GET"; //diff == 0
            } else if (actionType < writeQuota) {
                id = writeIdsList.get(random.nextInt(writeIdsList.size()));
                action = "CHANGE";
                long rand = random.nextLong(-1000, 1000);
                producerMap.compute(id, (k, v) -> {
                    if (v != null) {
                        diff[0] = rand % v; //may be negative
                        return v + diff[0];
                    } else {
                        diff[0] = Math.abs(rand);
                        return diff[0];
                    }
                });
            } else {
                id = -1;
                action = "OTHER";
            }
            return new Balance(id, diff[0], action);
        }));

        //producer flow for Balance events
        final Flux<Long> intervalChange = Flux.interval(
                Duration.ofMillis(random.nextLong(startDelay)), Duration.ofMillis(period));
        Flux.zip(intervalChange, balanceEvents)
                .map(it ->
                        {
                            Balance event = it.getT2();
                            switch (event.getAction()) {
                                case "GET" -> log.debug("[PRODUCER] GET {}", event.getId());
                                case "CHANGE" -> log.info("[PRODUCER] CHANGE ({},{})", event.getId(), event.getDiff());
                                default -> log.info("[PRODUCER] OTHER");
                            }
                            return balanceTemplate.send("balance-avro", event.getId(), event);
                        }
                ).blockLast();
    }
}
