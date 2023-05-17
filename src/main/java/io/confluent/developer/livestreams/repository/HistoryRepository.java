package io.confluent.developer.livestreams.repository;

import io.confluent.developer.livestreams.model.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findByAccountId(long accountId);
}
