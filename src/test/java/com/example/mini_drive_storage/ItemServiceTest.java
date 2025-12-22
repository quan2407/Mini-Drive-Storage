package com.example.mini_drive_storage;

import com.example.mini_drive_storage.dto.CreateFolderRequest;
import com.example.mini_drive_storage.dto.ItemResponseDto;
import com.example.mini_drive_storage.dto.ShareFileRequest;
import com.example.mini_drive_storage.entity.FilePermission;
import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.enums.ItemType;
import com.example.mini_drive_storage.enums.PermissionLevel;
import com.example.mini_drive_storage.exception.InvalidRequestException;
import com.example.mini_drive_storage.repo.FilePermissionRepo;
import com.example.mini_drive_storage.repo.ItemRepo;
import com.example.mini_drive_storage.repo.UserRepo;
import com.example.mini_drive_storage.service.EmailService;
import com.example.mini_drive_storage.service.ItemService;
import com.example.mini_drive_storage.utils.CurrentUserUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private ItemRepo itemRepo;

    @Mock
    private FilePermissionRepo filePermissionRepo;
    @Mock
    private CurrentUserUtils currentUserUtils;
    @Mock
    private UserRepo userRepo;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ItemService itemService;

    @Test
    void createFolder_root_success() {
        //ARRANGE
        Users user = Users.builder()
                .id(UUID.randomUUID())
                .email("test@gmail.com")
                .build();
        CreateFolderRequest createFolderRequest = CreateFolderRequest.builder()
                .name("My folder")
                .type(ItemType.FOLDER)
                .parentId(null)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.save(any(Items.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //ACT
        ItemResponseDto result = itemService.createFolder(createFolderRequest);

        //ASSERT
        assertNotNull(result);
        assertEquals("My folder", result.getName());

        verify(itemRepo).save(any(Items.class));
        verify(filePermissionRepo).save(any(FilePermission.class));
    }

    @Test
    void createFolder_typeNotFolder_shouldThrow() {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Invalid")
                .type(ItemType.FILE)
                .build();

        assertThrows(InvalidRequestException.class, () -> {
            itemService.createFolder(request);
        });
    }

    @Test
    void createFolder_withParent_success() {
        Users user = Users.builder()
                .id(UUID.randomUUID())
                .email("test@gmail.com")
                .build();

        Items parent = Items.builder()
                .id(UUID.randomUUID())
                .type(ItemType.FOLDER)
                .owner(user)
                .build();

        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Child Folder")
                .type(ItemType.FOLDER)
                .parentId(parent.getId())
                .build();
        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(itemRepo.save(any(Items.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FilePermission parentPermission = FilePermission.builder()
                .sharedToUser(user)
                .permissionLevel(PermissionLevel.EDIT)
                .build();
        when(filePermissionRepo.findByItem(parent)).thenReturn(List.of(parentPermission));

        ItemResponseDto result = itemService.createFolder(request);

        assertNotNull(result);
        assertEquals("Child Folder", result.getName());
        verify(itemRepo).findById(parent.getId());
        verify(filePermissionRepo).findByItem(parent);
        verify(filePermissionRepo).save(any(FilePermission.class));
    }

    @Test
    void createFolder_noEditPermission_shouldThrow() {
        Users user = Users.builder()
                .id(UUID.randomUUID())
                .build();

        Items parent = Items.builder()
                .id(UUID.randomUUID())
                .type(ItemType.FOLDER)
                .owner(Users.builder().id(UUID.randomUUID()).build())
                .build();

        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Child")
                .type(ItemType.FOLDER)
                .parentId(parent.getId())
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(filePermissionRepo.findByItemAndSharedToUser(parent, user))
                .thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () ->
                itemService.createFolder(request)
        );
    }

    @Test
    void uploadFiles_root_success() throws Exception {
        Users user = Users.builder()
                .id(UUID.randomUUID())
                .email("test@gmail.com")
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(user);

        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("hello".getBytes()));
        when(itemRepo.save(any(Items.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = itemService.uploadFiles(List.of(file), null);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.size());
        assertEquals("test.txt", result.get(0).getName());
        verify(itemRepo).save(any(Items.class));
        verify(filePermissionRepo).save(any(FilePermission.class));
    }

    @Test
    void uploadFiles_nullFiles_shouldThrow() {
        when(currentUserUtils.getCurrentUser())
                .thenReturn(Users.builder().id(UUID.randomUUID()).build());

        assertThrows(InvalidRequestException.class, () ->
                itemService.uploadFiles(null, null)
        );
    }

    @Test
    void uploadFiles_emptyFiles_shouldThrow() {
        when(currentUserUtils.getCurrentUser())
                .thenReturn(Users.builder().id(UUID.randomUUID()).build());

        assertThrows(InvalidRequestException.class, () ->
                itemService.uploadFiles(java.util.List.of(), null)
        );
    }
    @Test
    void uploadFiles_parentNotFolder_shouldThrow() {
        Users user = Users.builder().id(UUID.randomUUID()).build();

        Items parent = Items.builder()
                .id(UUID.randomUUID())
                .type(ItemType.FILE)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        assertThrows(InvalidRequestException.class, () ->
                itemService.uploadFiles(List.of(file), parent.getId())
        );
    }

    @Test
    void uploadFiles_noEditPermission_shouldThrow() {
        Users user = Users.builder().id(UUID.randomUUID()).build();

        Items parent = Items.builder()
                .id(UUID.randomUUID())
                .type(ItemType.FOLDER)
                .owner(Users.builder().id(UUID.randomUUID()).build())
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        when(filePermissionRepo.findByItemAndSharedToUser(parent, user))
                .thenReturn(Optional.empty());

        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        assertThrows(InvalidRequestException.class, () ->
                itemService.uploadFiles(List.of(file), parent.getId())
        );
    }

    @Test
    void shareItem_owner_canShare_success() {
        // ARRANGE
        Users owner = Users.builder()
                .id(UUID.randomUUID())
                .email("owner@gmail.com")
                .build();

        Users target = Users.builder()
                .id(UUID.randomUUID())
                .email("target@gmail.com")
                .build();

        Items item = Items.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .build();

        ShareFileRequest request = ShareFileRequest.builder()
                .email("target@gmail.com")
                .permission(PermissionLevel.VIEW)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(owner);
        when(itemRepo.findById(item.getId())).thenReturn(Optional.of(item));
        when(userRepo.findByEmail("target@gmail.com"))
                .thenReturn(Optional.of(target));

        when(filePermissionRepo.save(any(FilePermission.class)))
                .thenAnswer(i -> i.getArgument(0));

        // ACT
        itemService.shareItem(item.getId(), request);

        // ASSERT
        verify(filePermissionRepo).save(any(FilePermission.class));
        verify(emailService).sendShareNotification(any(), any(),any());
    }

    @Test
    void shareItem_nonOwner_noEditPermission_shouldThrow() {
        Users owner = Users.builder()
                .id(UUID.randomUUID())
                .email("owner@gmail.com")
                .build();

        Users user = Users.builder()
                .id(UUID.randomUUID())
                .email("user@gmail.com")
                .build();

        Users target = Users.builder()
                .id(UUID.randomUUID())
                .email("target@gmail.com")
                .build();

        Items item = Items.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .type(ItemType.FILE)
                .build();

        FilePermission viewPermission = FilePermission.builder()
                .item(item)
                .sharedToUser(user)
                .permissionLevel(PermissionLevel.VIEW)
                .build();

        ShareFileRequest request = ShareFileRequest.builder()
                .email("target@gmail.com")
                .permission(PermissionLevel.VIEW)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.findById(item.getId())).thenReturn(Optional.of(item));
        when(filePermissionRepo.findByItemAndSharedToUser(item, user))
                .thenReturn(Optional.of(viewPermission));

        assertThrows(InvalidRequestException.class, () ->
                itemService.shareItem(item.getId(), request)
        );

        verify(filePermissionRepo, never()).save(any());
        verify(emailService, never()).sendShareNotification(any(), any(), any());
    }

    @Test
    void shareItem_nonOwner_editPermission_success() {
        Users owner = Users.builder()
                .id(UUID.randomUUID())
                .email("owner@gmail.com")
                .build();

        Users editor = Users.builder()
                .id(UUID.randomUUID())
                .email("editor@gmail.com")
                .build();

        Users target = Users.builder()
                .id(UUID.randomUUID())
                .email("target@gmail.com")
                .build();

        Items item = Items.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .type(ItemType.FILE)
                .build();

        FilePermission editPermission = FilePermission.builder()
                .item(item)
                .sharedToUser(editor)
                .permissionLevel(PermissionLevel.EDIT)
                .build();

        ShareFileRequest request = ShareFileRequest.builder()
                .email("target@gmail.com")
                .permission(PermissionLevel.VIEW)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(editor);
        when(itemRepo.findById(item.getId())).thenReturn(Optional.of(item));
        when(filePermissionRepo.findByItemAndSharedToUser(item, editor))
                .thenReturn(Optional.of(editPermission));
        when(userRepo.findByEmail("target@gmail.com"))
                .thenReturn(Optional.of(target));

        itemService.shareItem(item.getId(), request);

        verify(filePermissionRepo).save(any(FilePermission.class));
        verify(emailService).sendShareNotification(
                eq("target@gmail.com"),
                eq(item.getName()),
                eq(PermissionLevel.VIEW)
        );
    }

    @Test
    void shareItem_targetEmailNotFound_shouldThrow() {
        Users owner = Users.builder()
                .id(UUID.randomUUID())
                .email("owner@gmail.com")
                .build();

        Items item = Items.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .type(ItemType.FILE)
                .build();

        ShareFileRequest request = ShareFileRequest.builder()
                .email("notfound@gmail.com")
                .permission(PermissionLevel.VIEW)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(owner);
        when(itemRepo.findById(item.getId())).thenReturn(Optional.of(item));
        when(userRepo.findByEmail("notfound@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () ->
                itemService.shareItem(item.getId(), request)
        );

        verify(filePermissionRepo, never()).save(any());
        verify(emailService, never()).sendShareNotification(any(), any(), any());
    }

    @Test
    void shareFolder_recursivePermissionCreated() {
        Users owner = Users.builder()
                .id(UUID.randomUUID())
                .email("owner@gmail.com")
                .build();

        Users target = Users.builder()
                .id(UUID.randomUUID())
                .email("target@gmail.com")
                .build();

        Items rootFolder = Items.builder()
                .id(UUID.randomUUID())
                .name("root")
                .type(ItemType.FOLDER)
                .owner(owner)
                .build();

        Items childFile = Items.builder()
                .id(UUID.randomUUID())
                .name("file.txt")
                .type(ItemType.FILE)
                .parent(rootFolder)
                .build();

        Items childFolder = Items.builder()
                .id(UUID.randomUUID())
                .name("sub")
                .type(ItemType.FOLDER)
                .parent(rootFolder)
                .build();

        ShareFileRequest request = ShareFileRequest.builder()
                .email("target@gmail.com")
                .permission(PermissionLevel.VIEW)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(owner);
        when(itemRepo.findById(rootFolder.getId()))
                .thenReturn(Optional.of(rootFolder));
        when(userRepo.findByEmail("target@gmail.com"))
                .thenReturn(Optional.of(target));

        when(itemRepo.findByParent(rootFolder))
                .thenReturn(List.of(childFile, childFolder));

        when(itemRepo.findByParent(childFolder))
                .thenReturn(List.of());

        itemService.shareItem(rootFolder.getId(), request);

        verify(filePermissionRepo, atLeast(3)).save(any(FilePermission.class));
        verify(emailService).sendShareNotification(any(), any(), any());
    }

    @Test
    void softDelete_owner_success() {
        Users owner = Users.builder()
                .id(UUID.randomUUID())
                .build();

        Items item = Items.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(owner);
        when(itemRepo.findById(item.getId()))
                .thenReturn(Optional.of(item));

        itemService.softDelete(item.getId());

        assertNotNull(item.getDeletedAt());
        verify(itemRepo).save(item);
    }

    @Test
    void softDelete_itemNotFound_shouldThrow() {
        UUID itemId = UUID.randomUUID();

        when(currentUserUtils.getCurrentUser())
                .thenReturn(Users.builder().id(UUID.randomUUID()).build());

        when(itemRepo.findById(itemId))
                .thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () ->
                itemService.softDelete(itemId)
        );

        verify(itemRepo, never()).save(any());
    }

    @Test
    void softDelete_nonOwner_viewPermission_shouldThrow() {
        Users owner = Users.builder().id(UUID.randomUUID()).build();
        Users user  = Users.builder().id(UUID.randomUUID()).build();

        Items item = Items.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .build();

        FilePermission viewPermission = FilePermission.builder()
                .item(item)
                .sharedToUser(user)
                .permissionLevel(PermissionLevel.VIEW)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(user);
        when(itemRepo.findById(item.getId()))
                .thenReturn(Optional.of(item));
        when(filePermissionRepo.findByItemAndSharedToUser(item, user))
                .thenReturn(Optional.of(viewPermission));

        assertThrows(InvalidRequestException.class, () ->
                itemService.softDelete(item.getId())
        );

        verify(itemRepo, never()).save(any());
    }

    @Test
    void softDelete_nonOwner_editPermission_success() {
        Users owner = Users.builder().id(UUID.randomUUID()).build();
        Users editor = Users.builder().id(UUID.randomUUID()).build();

        Items item = Items.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .build();

        FilePermission editPermission = FilePermission.builder()
                .item(item)
                .sharedToUser(editor)
                .permissionLevel(PermissionLevel.EDIT)
                .build();

        when(currentUserUtils.getCurrentUser()).thenReturn(editor);
        when(itemRepo.findById(item.getId()))
                .thenReturn(Optional.of(item));
        when(filePermissionRepo.findByItemAndSharedToUser(item, editor))
                .thenReturn(Optional.of(editPermission));

        itemService.softDelete(item.getId());

        assertNotNull(item.getDeletedAt());
        verify(itemRepo).save(item);
    }

}
