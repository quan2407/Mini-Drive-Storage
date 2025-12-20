package com.example.mini_drive_storage.repo;

import com.example.mini_drive_storage.entity.FilePermission;
import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FilePermissionRepo extends JpaRepository<FilePermission, UUID> {
    List<FilePermission> findByItem(Items parent);

    Optional<FilePermission> findByItemAndSharedToUser(Items item, Users user);

    List<FilePermission> findBySharedToUser(Users user);

    void deleteByItem(Items item);
}
