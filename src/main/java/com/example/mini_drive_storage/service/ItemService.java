package com.example.mini_drive_storage.service;

import com.example.mini_drive_storage.dto.CreateFolderRequest;
import com.example.mini_drive_storage.dto.ItemResponseDto;
import com.example.mini_drive_storage.entity.FilePermission;
import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.enums.ItemType;
import com.example.mini_drive_storage.enums.PermissionLevel;
import com.example.mini_drive_storage.exception.InvalidRequestException;
import com.example.mini_drive_storage.exception.NotFoundException;
import com.example.mini_drive_storage.repo.FilePermissionRepo;
import com.example.mini_drive_storage.repo.ItemRepo;
import com.example.mini_drive_storage.utils.CurrentUserUtils;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ItemService {
    private ItemRepo itemRepo;
    private FilePermissionRepo filePermissionRepo;
    private final CurrentUserUtils currentUserUtils;

    @PostConstruct // when bean service created successfully, this method will start once
    public void initStorage() {
        try {
            Path root = Paths.get(UPLOAD_ROOT);
            if (Files.notExists(root)) {
                Files.createDirectories(root);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }
    private static final String UPLOAD_ROOT = "storage";

    private void checkEditPermission(Items item, Users user) {
        FilePermission permission = (FilePermission) filePermissionRepo
                .findByItemAndSharedToUser(item, user)
                .orElseThrow(() ->
                        new InvalidRequestException("You don't have permission to upload to this folder"));

        if (permission.getPermissionLevel() != PermissionLevel.EDIT) {
            throw new InvalidRequestException("You don't have edit permission");
        }
    }

    private void createInitialPermissions(Items item, Items parent, Users owner) {
        if (parent == null) {
            FilePermission permission = FilePermission.builder()
                    .item(item)
                    .sharedToUser(owner)
                    .permissionLevel(PermissionLevel.EDIT)
                    .inherited(false)
                    .build();
            filePermissionRepo.save(permission);
        } else {
            List<FilePermission> parentPermissions = filePermissionRepo.findByItem(parent);
            for (FilePermission p : parentPermissions) {
                FilePermission childPerm = FilePermission.builder()
                        .item(item)
                        .sharedToUser(p.getSharedToUser())
                        .permissionLevel(p.getPermissionLevel())
                        .inherited(true)
                        .build();
                filePermissionRepo.save(childPerm);
            }
        }
    }

    public List<ItemResponseDto> uploadFiles(List<MultipartFile> files, UUID parentId) {
        Users currentUser = currentUserUtils.getCurrentUser();

        List<ItemResponseDto> itemResponseDtos = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            throw new InvalidRequestException("File is empty");
        }
        // check parent folder
        Items parent = null;

        if (parentId != null) {
            parent = itemRepo.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("Parent folder not found"));

            if (parent.getType() != ItemType.FOLDER) {
                throw new InvalidRequestException("Parent is not folder");
            }

        }
        // if these file in a parent folder, check parent folder have permission edit to current user
        if (parent != null) {
            checkEditPermission(parent, currentUser);
        }

// if these files don't belong to any parent folder => store to the root
        Path parentDir = parent == null
                ? Paths.get(UPLOAD_ROOT)
                : Paths.get(UPLOAD_ROOT, parent.getId().toString());
        if (Files.notExists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                throw new RuntimeException("Could not initialize storage", e);
            }
        }

        for (MultipartFile file : files) {
            if (file.getSize() == 0) continue;
            String originalFilename = file.getOriginalFilename();
            String storageFileName = UUID.randomUUID().toString() + "_" + originalFilename;
            Path filePath = parentDir.resolve(storageFileName);
            try {
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Could not upload file", e);
            }
            Items items = Items.builder()
                    .name(originalFilename)
                    .type(ItemType.FILE)
                    .size(file.getSize())
                    .path(filePath.toString())
                    .mimeType(file.getContentType())
                    .parent(parent)
                    .owner(currentUser)
                    .build();
            Items savedItem = itemRepo.save(items);
            createInitialPermissions(savedItem, parent, currentUser);
            itemResponseDtos.add(ItemResponseDto.from(savedItem));
        }
return itemResponseDtos;
    }

    public ItemResponseDto createFolder(CreateFolderRequest createFolderRequest) {
        if (createFolderRequest.getType() != ItemType.FOLDER) {
            throw new InvalidRequestException("Type is not folder");
        }
        Users currentUser = currentUserUtils.getCurrentUser();
        Items parent = null;
        if(createFolderRequest.getParentId() != null){
            parent = itemRepo.findById(createFolderRequest.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent folder not found"));
        }
        if (parent != null && parent.getType() != ItemType.FOLDER) {
            throw new InvalidRequestException("Parent is not folder");
        }
        // if this folder in a parent folder, check parent folder have permission edit to current user
        checkEditPermission(parent, currentUser);
        Items folder = Items.builder()
                .name(createFolderRequest.getName())
                .parent(parent)
                .type(ItemType.FOLDER)
                .owner(currentUser)
                .build();
        Items savedItem = itemRepo.save(folder);
        createInitialPermissions(savedItem, parent, currentUser);
        return ItemResponseDto.from(savedItem);
    }
}
