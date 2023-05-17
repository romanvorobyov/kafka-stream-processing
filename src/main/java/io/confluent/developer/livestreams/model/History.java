package io.confluent.developer.livestreams.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "history_gen")
    @SequenceGenerator(name = "history_gen", sequenceName = "history_seq", allocationSize = 1)
    @Column(updatable = false, nullable = false)
    private long id;

    @Column(updatable = false, nullable = false)
    private long diff;

    @Column(updatable = false, nullable = false)
    private long value;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime eventTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", updatable = false, nullable = false)
    private Account account;
}