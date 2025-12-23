package com.example.mini_drive_storage.service;

import com.example.mini_drive_storage.dto.*;
import com.example.mini_drive_storage.entity.FilePermission;
import com.example.mini_drive_storage.entity.FolderDownloadStatus;
import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.enums.ItemType;
import com.example.mini_drive_storage.enums.PermissionLevel;
import com.example.mini_drive_storage.exception.InvalidRequestException;
import com.example.mini_drive_storage.exception.NotFoundException;
import com.example.mini_drive_storage.repo.FilePermissionRepo;
import com.example.mini_drive_storage.repo.ItemRepo;
import com.example.mini_drive_storage.repo.UserRepo;
import com.example.mini_drive_storage.utils.CurrentUserUtils;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Permission;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@AllArgsConstructor
public class ItemService {
    private final UserRepo userRepo;
    private final EmailService emailService;
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
        if (item.getOwner().equals(user)) {
            return;
        }
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
        if (createFolderRequest.getParentId() != null) {
            parent = itemRepo.findById(createFolderRequest.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent folder not found"));
        }
        if (parent != null && parent.getType() != ItemType.FOLDER) {
            throw new InvalidRequestException("Parent is not folder");
        }
        // if this folder in a parent folder, check parent folder have permission edit to current user
        if (parent != null) {
            checkEditPermission(parent, currentUser);
        }

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

    public ResponseEntity<?> downloadFile(UUID id) {
        Items item = itemRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (item.getType() != ItemType.FILE) {
            throw new InvalidRequestException("Item is not file");
        }

        Users currentUser = currentUserUtils.getCurrentUser();

        // if this file is shared to this user, user can download this file;
        // owner can download file
        if (!item.getOwner().getId().equals(currentUser.getId())) {

            FilePermission permission = filePermissionRepo
                    .findByItemAndSharedToUser(item, currentUser)
                    .orElseThrow(() -> new InvalidRequestException("No permission to download"));

            if (permission.getPermissionLevel() != PermissionLevel.VIEW
                    && permission.getPermissionLevel() != PermissionLevel.EDIT) {
                throw new InvalidRequestException("No permission to download");
            }
        }

        // check if this file is exist on this storage app
        Path path = Paths.get(item.getPath());
        if (Files.notExists(path)) {
            throw new NotFoundException("File not found");
        }

        // input stream use to read binary file: pdf,doc,zip,...
        InputStreamResource resource;
        try {
            resource = new InputStreamResource(Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException("Could not open file", e);
        }

        String contentType = item.getMimeType() != null
                ? item.getMimeType()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                // Content-Disposition tell the user how to resolve data
                // attachment mean force to download
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + item.getName() + "\"")
                .body(resource);
    }

    private final Map<UUID, FolderDownloadStatus> folderDownloadMap = new ConcurrentHashMap<>();

    public FolderDownloadStatus getFolderDownloadStatus(UUID requestId) {
        return folderDownloadMap.get(requestId);
    }

    public UUID triggerAsyncZip(UUID id) {
        Items item = itemRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        if (item.getType() != ItemType.FOLDER) {
            throw new InvalidRequestException("Item is not folder");
        }
        Users currentUser = currentUserUtils.getCurrentUser();
        // if this folder is shared to this user, user can download this folder;
        FilePermission permission = (FilePermission) filePermissionRepo.findByItemAndSharedToUser(item, currentUser)
                .orElseThrow(() -> new InvalidRequestException("No permission to download"));
        UUID requestId = UUID.randomUUID();
        folderDownloadMap.put(requestId, new FolderDownloadStatus("PENDING", null));

        zipFolderAsync(item, requestId);

        return requestId;
    }

    @Async
    public void zipFolderAsync(Items item, UUID requestId) {
        FolderDownloadStatus folderDownloadStatus = folderDownloadMap.get(requestId);
        folderDownloadStatus.setStatus("PROCESSING");

        try {
            Path zipPath = Paths.get(UPLOAD_ROOT, requestId + ".zip").toAbsolutePath();
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                addFolderToZip(item, item.getName(), zos);
            }
            folderDownloadStatus.setZipPath(zipPath.toString().replace("\\", "/"));
            folderDownloadStatus.setStatus("READY");
        } catch (Exception e) {
            folderDownloadStatus.setStatus("FAILED");
            e.printStackTrace();
        }
    }

    private void addFolderToZip(Items folder, String parentPath, ZipOutputStream zos) throws IOException {
        List<Items> children = itemRepo.findByParent(folder);

        for (Items child : children) {
            if (child.getType() == ItemType.FILE) {
                Path filePath = Paths.get(child.getPath());
                try (InputStream is = Files.newInputStream(filePath)) {
                    ZipEntry zipEntry = new ZipEntry(parentPath + "/" + child.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = is.read(bytes)) != -1) {
                        zos.write(bytes, 0, length);
                    }
                    zos.closeEntry();
                }
            } else if (child.getType() == ItemType.FOLDER) {
                addFolderToZip(child, parentPath + "/" + child.getName(), zos);
            }
        }
    }

    public ResponseEntity<?> downloadFolderZip(UUID requestId) throws IOException {
        FolderDownloadStatus status = folderDownloadMap.get(requestId);

        if (status == null) {
            throw new NotFoundException("RequestId not found");
        }

        if (!"READY".equals(status.getStatus())) {
            throw new InvalidRequestException("File is not ready to download");
        }

        Path path = Paths.get(status.getZipPath());

        if (Files.notExists(path)) {
            throw new NotFoundException("Zip file not found on disk");
        }

        InputStreamResource resource = new InputStreamResource(Files.newInputStream(path));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }


    public ResponseEntity<?> shareItem(UUID id, @Valid ShareFileRequest shareFileRequest) {
        Users currentUser = currentUserUtils.getCurrentUser();

        Items item = itemRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        boolean isOwner = item.getOwner().getId().equals(currentUser.getId());

        if (!isOwner) {
            FilePermission permission = filePermissionRepo
                    .findByItemAndSharedToUser(item, currentUser)
                    .orElseThrow(() ->
                            new InvalidRequestException("You don't have permission to share this item"));

            if (permission.getPermissionLevel() != PermissionLevel.EDIT) {
                throw new InvalidRequestException("You don't have permission to share this item");
            }
        }

        Users shareUser = userRepo.findByEmail(shareFileRequest.getEmail())
                .orElseThrow(() -> new InvalidRequestException("Email not found"));

        PermissionLevel permission = shareFileRequest.getPermission();

        upsertPermission(item, shareUser, permission, false);
        emailService.sendShareNotification(
                shareUser.getEmail(),
                item.getName(),
                permission
        );

        if (item.getType() == ItemType.FOLDER) {
            shareFileRecursive(item, shareUser, permission);
        }

        return ResponseEntity.ok("Item shared successfully");
    }


    private void upsertPermission(
            Items item,
            Users shareUser,
            PermissionLevel permission,
            boolean inherited
    ) {
        Optional<FilePermission> existing =
                filePermissionRepo.findByItemAndSharedToUser(item, shareUser);

        if (existing.isPresent()) {
            FilePermission fp = existing.get();
            fp.setPermissionLevel(permission);
            fp.setInherited(inherited);
            filePermissionRepo.save(fp);
        } else {
            FilePermission fp = FilePermission.builder()
                    .item(item)
                    .sharedToUser(shareUser)
                    .permissionLevel(permission)
                    .inherited(inherited)
                    .build();
            filePermissionRepo.save(fp);
        }
    }

private void shareFileRecursive(Items item, Users shareUser, PermissionLevel permission) {
        List<Items> children = itemRepo.findByParent(item);
        for (Items child : children) {
            upsertPermission(child,shareUser,permission,true);
            if(child.getType().equals(ItemType.FOLDER)) {
                shareFileRecursive(child,shareUser,permission);
            }
        }
}

    public List<SharedItemResponseDto> getSharedItemForCurrentUser() {
        Users currentUser = currentUserUtils.getCurrentUser();

        return filePermissionRepo.findBySharedToUser(currentUser)
                .stream()
                .map(SharedItemResponseDto::from)
                .toList();
    }

    public List<ItemResponseDto> search(FileSearchRequest fileSearchRequest) {
        Users currentUser = currentUserUtils.getCurrentUser();

        ItemType itemType = null;
        String mimeType = null;

        if (fileSearchRequest.getType() != null) {
            try {
                itemType = ItemType.valueOf(fileSearchRequest.getType());

            } catch (IllegalArgumentException e) {
                mimeType = fileSearchRequest.getType();
            }
        }
        List<Items> items = itemRepo.search(
                currentUser.getId(),
                fileSearchRequest.getQ(),
                mimeType,
                String.valueOf(itemType),
                fileSearchRequest.getParentId(),
                fileSearchRequest.getFromSize(),
                fileSearchRequest.getToSize()
        );
        return items.stream().map(ItemResponseDto::from).toList();
    }

    public UsageAnalyticsResponse getUsage() {
        Users currentUser = currentUserUtils.getCurrentUser();
        List<Object[]> results = itemRepo.getUsageStats(currentUser);
        Object[] row = results.get(0);
        long totalFiles = ((Number) row[0]).longValue();
        long totalSize  = ((Number) row[1]).longValue();
        return UsageAnalyticsResponse.builder()
                .totalFiles(totalFiles)
                .totalSize(totalSize)
                .build();
    }
@Transactional
    public void softDelete(UUID id) {
        Users currentUser = currentUserUtils.getCurrentUser();
        Items item = itemRepo.findById(id).orElseThrow(() -> new InvalidRequestException("Item not found"));
        checkEditPermission(item,currentUser);
        item.setDeletedAt(Instant.now());
        itemRepo.save(item);
    }
}
