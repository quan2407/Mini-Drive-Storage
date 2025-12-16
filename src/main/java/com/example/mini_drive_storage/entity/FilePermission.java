package com.example.mini_drive_storage.entity;
import com.example.mini_drive_storage.enums.PermissionLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "file_permission",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"item_id", "user_id"})
        }
)

@EntityListeners(AuditingEntityListener.class)
@Builder
public class FilePermission {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Items item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users sharedToUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionLevel permissionLevel;

    private boolean inherited;

    @CreatedDate
    private Instant createdDate;
}
