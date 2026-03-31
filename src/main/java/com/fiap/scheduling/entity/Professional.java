package com.fiap.scheduling.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "professionals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Professional {

    @Id
    private UUID id;

    @PrePersist
    public void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String crm;
}
