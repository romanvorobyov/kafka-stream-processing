package io.confluent.developer.livestreams.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Account {

    @Id
    @Column(updatable = false, nullable = false)
    private long id;

    @Column(nullable = false)
    private long value;

    @Version
    private short version;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime created;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updated;

    @OneToMany(mappedBy = "account")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OrderBy(value = "eventTime ASC")
    List<History> histories;
}