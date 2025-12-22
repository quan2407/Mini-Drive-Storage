package com.example.mini_drive_storage;

import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.enums.PermissionLevel;
import com.example.mini_drive_storage.repo.FilePermissionRepo;
import com.example.mini_drive_storage.repo.ItemRepo;
import com.example.mini_drive_storage.service.PermissionService;
import com.example.mini_drive_storage.utils.CurrentUserUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
// FAKE ITEM REPO
    @Mock
    private ItemRepo itemRepo;

    @Mock
    private FilePermissionRepo filePermissionRepo;

    @Mock
    private CurrentUserUtils currentUserUtils;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void canEditItem_owner_shouldReturnTrue() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);

        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.existsByIdAndOwnerId(itemId, userId)).thenReturn(true);

        boolean result = permissionService.canEditItem(itemId);
        assertTrue(result);
    }

    @Test
    void canEditItem_sharedWithEdit_shouldReturnTrue() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);
        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.existsByIdAndOwnerId(itemId, userId)).thenReturn(false);
        when(filePermissionRepo.existsByItemIdAndSharedToUserIdAndPermissionLevel(itemId, userId, PermissionLevel.EDIT)).thenReturn(true);
        boolean result = permissionService.canEditItem(itemId);
        assertTrue(result);
    }
    @Test
    void canEditItem_noPermission_shouldReturnFalse() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);
        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.existsByIdAndOwnerId(itemId, userId)).thenReturn(false);
        when(filePermissionRepo.existsByItemIdAndSharedToUserIdAndPermissionLevel(itemId, userId, PermissionLevel.EDIT)).thenReturn(false);
        boolean result = permissionService.canEditItem(itemId);
        assertFalse(result);

    }

    @Test
    void canViewItem_shared_shouldReturnTrue() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);
        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.existsByIdAndOwnerId(itemId, userId)).thenReturn(false);
        when(filePermissionRepo.existsByItemIdAndSharedToUserId(itemId, userId)).thenReturn(true);
        boolean result = permissionService.canViewItem(itemId);
        assertTrue(result);
    }

    @Test
    void canViewItem_shared_shouldReturnFalse() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);
        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.existsByIdAndOwnerId(itemId, userId)).thenReturn(false);
        when(filePermissionRepo.existsByItemIdAndSharedToUserId(itemId, userId)).thenReturn(false);
        boolean result = permissionService.canViewItem(itemId);
        assertFalse(result);
    }
}
