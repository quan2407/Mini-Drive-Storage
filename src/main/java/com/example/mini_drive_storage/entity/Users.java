package com.example.mini_drive_storage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
public class Users {
    @Id
    @UuidGenerator
    private UUID id;
    private String fullName;
    @Column(unique = true)
    private String email;
    private String password;
    @CreatedDate
    private Instant createdDate;
    @LastModifiedDate
    private Instant updatedDate;
}
