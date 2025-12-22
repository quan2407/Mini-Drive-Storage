package com.example.mini_drive_storage.service;
import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.enums.PermissionLevel;
import com.example.mini_drive_storage.repo.FilePermissionRepo;
import com.example.mini_drive_storage.repo.ItemRepo;
import com.example.mini_drive_storage.utils.CurrentUserUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class PermissionService {
    private final ItemRepo itemRepo;
    private final FilePermissionRepo filePermissionRepo;
    private final CurrentUserUtils currentUserUtils;

    public boolean canEditItem(UUID itemId) {
        Users currentUser = currentUserUtils.getCurrentUser();
        if (itemRepo.existsByIdAndOwnerId(itemId,currentUser.getId())){
            return true;
        }
        return filePermissionRepo.existsByItemIdAndSharedToUserIdAndPermissionLevel(
                itemId, currentUser.getId(), PermissionLevel.EDIT
        );
    }
    public boolean canViewItem(UUID itemId) {
        Users currentUser = currentUserUtils.getCurrentUser();
        if (itemRepo.existsByIdAndOwnerId(itemId,currentUser.getId())){
            return true;
        }
        return filePermissionRepo.existsByItemIdAndSharedToUserId(
                itemId, currentUser.getId()
        );    }
}
