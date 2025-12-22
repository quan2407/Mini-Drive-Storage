package com.example.mini_drive_storage.controller;

import com.example.mini_drive_storage.dto.*;
import com.example.mini_drive_storage.entity.FolderDownloadStatus;
import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.service.ItemService;
import com.example.mini_drive_storage.service.PermissionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class ItemController {
    private ItemService itemService;

    @PostMapping(
            value = "/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<ItemResponseDto>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "parentId", required = false) UUID parentId
    ) {
        List<ItemResponseDto> items = itemService.uploadFiles(files, parentId);
        return ResponseEntity.ok(items);
    }

    @PostMapping(
            value = "/files",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ItemResponseDto> uploadFolder(
            @RequestBody CreateFolderRequest createFolderRequest
    ) {
        return ResponseEntity.ok(itemService.createFolder(createFolderRequest));
    }

    @PreAuthorize("@permissionService.canViewItem(#id)")
    @GetMapping("/{id}/download")
    // ? represent can accept any body type, because download api can return many type like Resource,JSON or no body
    public ResponseEntity<?> downloadFile(@PathVariable UUID id) {
        return itemService.downloadFile(id);
    }

    @PreAuthorize("@permissionService.canViewItem(#id)")
    @PostMapping("/{id}/download")
    public ResponseEntity<DownloadFolderResponse> downloadFolder(@PathVariable UUID id) {
        UUID requestId = itemService.triggerAsyncZip(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(DownloadFolderResponse.builder()
                        .requestId(requestId.toString())
                        .build());

    }

    @GetMapping("/downloads/{requestId}")
    public ResponseEntity<?> pollingFolder(@PathVariable UUID requestId) {
        FolderDownloadStatus status = itemService.getFolderDownloadStatus(requestId);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        if ("READY".equals(status.getStatus())) {
            body.put("downloadUrl", "/api/v1/files/downloads/" + requestId + "/file");
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/files/downloads/{requestId}/file")
    public ResponseEntity<?> downloadFolderZip(@PathVariable UUID requestId) throws IOException {
        System.out.println("downloadFolderZip");
        return itemService.downloadFolderZip(requestId);
    }

    @PreAuthorize("@permissionService.canEditItem(#id)")
    @PostMapping("/files/{id}/share")
    public ResponseEntity<?> sharedFile(@PathVariable UUID id, @Valid @RequestBody ShareFileRequest shareFileRequest) {
        return itemService.shareItem(id, shareFileRequest);
    }

    @GetMapping("/files/shared-with-me")
    public ResponseEntity<List<SharedItemResponseDto>> sharedWithMe() {
        return ResponseEntity.ok(itemService.getSharedItemForCurrentUser());
    }

    @GetMapping("/files")
    public ResponseEntity<?> searchFile(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) UUID parentId,
                                        @RequestParam(required = false) Long fromSize,
                                        @RequestParam(required = false) Long toSize) {
        FileSearchRequest fileSearchRequest = FileSearchRequest.builder()
                .q(q)
                .type(type)
                .parentId(parentId)
                .fromSize(fromSize)
                .toSize(toSize)
                .build();
        return ResponseEntity.ok(itemService.search(fileSearchRequest));
    }

    @GetMapping("/analytics/usage")
    public ResponseEntity<UsageAnalyticsResponse> analyticsUsage() {
return ResponseEntity.ok(itemService.getUsage());
    }

    @PreAuthorize("@permissionService.canEditItem(#id)")
    @DeleteMapping("/files/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable UUID id) {
        System.out.println("I am here");
        itemService.softDelete(id);
        return ResponseEntity.ok().build();
    }
}